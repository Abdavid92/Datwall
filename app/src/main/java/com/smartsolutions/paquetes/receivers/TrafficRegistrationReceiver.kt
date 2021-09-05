package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtils
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
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
class TrafficRegistrationReceiver @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: IAppRepository,
    private val userDataBytesManager: IUserDataBytesManager,
    private val networkUtils: NetworkUtils,
    private val simManager: ISimManager,
    private val trafficRepository: ITrafficRepository
): BroadcastReceiver(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

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
        lastTraffics.clear()
        lastTime = 0L
        LocalBroadcastManager.getInstance(context)
            .unregisterReceiver(this)
        isRegistered = false

        Log.i(TAG, "unregister: sending 0 bytes traffic band with")

        LocalBroadcastManager.getInstance(context).sendBroadcast(
            Intent(ACTION_TRAFFIC_REGISTRATION)
                .putExtra(EXTRA_TRAFFIC_TX, 0L)
                .putExtra(EXTRA_TRAFFIC_RX, 0L)
        )
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Watcher.ACTION_TICKTOCK) {
            //Resuelvo el ancho de banda
            val trafficResult = getAndSendBandWithTraffic()

            val currentTime = System.currentTimeMillis()

            launch {
                /*Este método se le debe pasar el currentTime como argumento porque
                el se demora un poco en hacer su trabajo y se pueden crear discordancias
                en el tiempo por esta demora.*/
                registerLollipopTraffic(
                    trafficResult.first,
                    trafficResult.second,
                    currentTime
                )
            }

            if (lastTime < currentTime - (DateUtils.MILLIS_PER_SECOND * 10)) {
                val start = lastTime
                lastTime = currentTime
                launch {
                    registerTraffic(start)
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

        getTrafficsToRegister(start).forEach { traffic ->
            apps.firstOrNull { it.uid == traffic.uid }?.let { app ->
                when (app.trafficType) {
                    TrafficType.International -> {
                        international += traffic
                    }
                    TrafficType.National -> {
                        national += traffic
                    }
                    TrafficType.Free -> {
                        //Ignored
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
     * Se encarga de obtener el trafico general y guardarlo en Lollipop.
     */
    private suspend fun registerLollipopTraffic(rx: Long, tx: Long, currentTime: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && rx > 0 && tx > 0) {
            val traffic = Traffic(
                NetworkUsageManager.GENERAL_TRAFFIC_UID,
                rx,
                tx,
                simManager.getDefaultDataSim().id
            ).apply {
                startTime = currentTime - DateUtils.MILLIS_PER_SECOND
                endTime = currentTime
            }
            trafficRepository.create(traffic)
        }
    }

    /**
     * Obtiene el ancho de banda de la red.
     * */
    private fun getAndSendBandWithTraffic(): Pair<Long, Long> {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()

        var pair = Pair(0L, 0L)

        if (rxBytes > 0 && txBytes > 0) {
            pair = Pair(rx - rxBytes, tx - txBytes)

            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent(ACTION_TRAFFIC_REGISTRATION)
                    .putExtra(EXTRA_TRAFFIC_RX, pair.first)
                    .putExtra(EXTRA_TRAFFIC_TX, pair.second))

            Log.i(TAG, "getAndSendBandWithTraffic: band with rx: ${pair.first} tx: ${pair.second}")
        }

        rxBytes = rx
        txBytes = tx

        return pair
    }

    private fun isLTE(): Boolean {
        return networkUtils.getNetworkGeneration() == NetworkUtils.NetworkType.NETWORK_4G
    }

    companion object {

        private const val TAG = "TrafficRegistration"

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

        @JvmStatic
        private var rxBytes = 0L

        @JvmStatic
        private var txBytes = 0L
    }

}