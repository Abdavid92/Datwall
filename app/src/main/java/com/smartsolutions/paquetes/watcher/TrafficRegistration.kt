package com.smartsolutions.paquetes.watcher

import android.net.TrafficStats
import android.os.Build
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class TrafficRegistration @Inject constructor(
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: IAppRepository,
    private val userDataBytesManager: IUserDataBytesManager,
    private val networkUtils: NetworkUtils,
    private val simManager: ISimManager,
    private val trafficRepository: ITrafficRepository,
    private val watcher: RxWatcher
) : CoroutineScope {

    private val mainJob = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + mainJob

    private var running = false

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

    fun start() {
        if (!running) {
            running = true

            launch {
                appRepository.flow().collect {
                    apps = it
                }
            }

            launch {
                watcher.bandWithFlow.collect {

                    if (it.first > 0 || it.second > 0) {
                        val currentTime = System.currentTimeMillis()

                        /*Este método se le debe pasar el currentTime como argumento porque
                     el se demora un poco en hacer su trabajo y se pueden crear discordancias
                     en el tiempo por esta demora.*/
                        registerLollipopTraffic(
                            it.first,
                            it.second,
                            currentTime
                        )

                        if (lastTime < currentTime - (DateUtils.MILLIS_PER_SECOND * 10)) {
                            val start = lastTime
                            lastTime = currentTime

                            registerTraffic(start)
                        }
                    }
                }
            }
        }
    }

    fun stop() {
        running = false
        mainJob.cancel()
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
            val sim = simManager.getDefaultSim(SimDelegate.SimType.DATA)
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
                simManager.getDefaultSim(SimDelegate.SimType.DATA).id
            ).apply {
                startTime = currentTime - DateUtils.MILLIS_PER_SECOND
                endTime = currentTime
            }
            trafficRepository.create(traffic)
        }
    }

    private fun isLTE(): Boolean {
        return networkUtils.getNetworkGeneration() == NetworkUtils.NetworkType.NETWORK_4G
    }
}