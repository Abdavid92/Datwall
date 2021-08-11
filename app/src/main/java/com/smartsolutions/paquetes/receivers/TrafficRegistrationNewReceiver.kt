package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class TrafficRegistrationNewReceiver @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: IAppRepository,
    private val userDataBytesManager: IUserDataBytesManager,
    private val networkUtil: NetworkUtil,
    private val simManager: ISimManager,
    private val trafficRepository: ITrafficRepository
): BroadcastReceiver(), CoroutineScope {

    var isRegistered = false
        private set

    /**
     * Tiempo de Inicio que se usará a partir de Android 6 con NetworkStatsManager para comenzar
     * a recopilar los datos
     */
    private val startTime = System.currentTimeMillis() - DateUtils.MILLIS_PER_HOUR

    /**
     * Lista de todas las apps instaladas que se actualizan dinamicamente mediante un flow en una
     * corroutina. Se usa para obtener el consumo de cada app
     */
    private var apps = listOf<App>()

    /**
     * Lista donde se guarda el trafico obtenido para poder compararlo con el siguiente y así sacar
     * la diferencia
     */
    private var lastTraffics = mutableListOf<Traffic>()

    /**
     * Indica la hora en que se realizó la ultima comparación y se obtuvo el ultimo trafico
     */
    private var lastTime = 0L


    init {
        launch {
            appRepository.flow().collect {
                apps = it
            }
        }
    }

    fun register() {
        if (!isRegistered) {
            LocalBroadcastManager.getInstance(context)
                .registerReceiver(this, IntentFilter(Watcher.ACTION_TICKTOCK))
            isRegistered = true
        }
    }

    fun unregister() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
        isRegistered = false
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Watcher.ACTION_TICKTOCK) {
            val currentTime = System.currentTimeMillis()
            if (lastTime < currentTime - (DateUtils.MILLIS_PER_SECOND * 10)) {
                launch {
                    getGeneralTrafficAndSendBroadcast(lastTime)
                    registerTraffic(lastTime)
                    lastTime = currentTime
                }
            }
        }
    }

    /**
     * Se encarga de obtener el trafico segun la version de Android y catalogarlo segun el tipo de
     * app que sea, national, international o free
     */
    private suspend fun registerTraffic(start: Long) {
        val international = Traffic()
        val national = Traffic()

        //TODO Falta por agregar un campo a el modelo App que permita marcarla como Free

        getTrafficsToRegister(start).forEach { traffic ->
            apps.firstOrNull { it.uid == traffic.uid }?.let { app ->
                when {
                    app.national -> {
                        national += traffic
                    }
                    else -> {
                        international += traffic
                    }
                }
            }
        }

        userDataBytesManager.registerTraffic(
            international._rxBytes,
            international._txBytes,
            national.totalBytes.bytes,
            isLTE()
        )
    }

    /**
     * Obtiene solamente el trafico realizado desde el ultimo tiempo solicitado y segun la linea
     * y la version de Android
     */
    private suspend fun getTrafficsToRegister(start: Long): List<Traffic> {
        val toRegister = mutableListOf<Traffic>()

        val traffics = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkUsageManager.getAppsUsage(startTime, System.currentTimeMillis())
        } else {
            val sim = simManager.getDefaultDataSim()
            val traffics = mutableListOf<Traffic>()

            apps.forEach { app ->
                traffics.add(Traffic(
                    app.uid,
                    TrafficStats.getUidRxBytes(app.uid),
                    TrafficStats.getUidTxBytes(app.uid),
                    sim.id
                ).apply {
                    network = if (isLTE()){
                        Networks.NETWORK_4G
                    }else {
                        Networks.NETWORK_3G
                    }

                    startTime = start
                    endTime = System.currentTimeMillis()
                })
            }

            traffics.toList()
        }

        traffics.forEach { traffic ->
            lastTraffics.firstOrNull { it.uid == traffic.uid && it.simId == traffic.simId }
                ?.let { lastTraffic ->
                    toRegister.add(traffic - lastTraffic)
                }
        }

        lastTraffics = traffics.toMutableList()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
            trafficRepository.create(toRegister)
        }

        return toRegister
    }

    /**
     * Se encarga de obtener el trafico general y guardarlo en Lollipop y de enviar el broadcast
     * para los demas servicios de la app
     */
    private suspend fun getGeneralTrafficAndSendBroadcast(start: Long) {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()

        if (rxBytes > 0 && txBytes > 0) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ACTION_TRAFFIC_REGISTRATION)
                    .putExtra(EXTRA_TRAFFIC_TX, tx - txBytes)
                    .putExtra(EXTRA_TRAFFIC_RX, rx - rxBytes)
            )

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                val traffic = Traffic(
                    NetworkUsageManager.GENERAL_TRAFFIC_UID,
                    rx - rxBytes,
                    tx - txBytes,
                    simManager.getDefaultDataSim().id
                ).apply {
                    startTime = start
                    endTime = System.currentTimeMillis()
                }
                trafficRepository.create(traffic)
            }

        }

        rxBytes = rx
        txBytes = tx

    }


    private fun isLTE(): Boolean {
        return networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default



    companion object {
        /**
         * Broadcast que se lanza cada un segundo para obtener el ancho de banda de la red.
         * */
        const val ACTION_TRAFFIC_REGISTRATION = "com.smartsolutions.paquetes.action.TRAFFIC_REGISTRATION"

        /**
         * Extra que contiene los bytes descargados.
         * */
        const val EXTRA_TRAFFIC_RX = "com.smartsolutions.paquetes.extra.TRAFFIC_RX"

        /**
         * Extra que contiene los bytes subidos.
         * */
        const val EXTRA_TRAFFIC_TX = "com.smartsolutions.paquetes.extra.TRAFFIC_TX"

        private var rxBytes = 0L
        private var txBytes = 0L
    }

}