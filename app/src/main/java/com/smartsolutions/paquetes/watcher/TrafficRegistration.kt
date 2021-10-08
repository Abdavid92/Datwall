package com.smartsolutions.paquetes.watcher

import android.net.TrafficStats
import android.os.Build
import android.util.Log
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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

    private var isRegistered = false

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private var running = false

    private var currentJob: Job? = null

    private var mainJob: Job? = null


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


    fun register() {
        if (!isRegistered) {
            isRegistered = true
            mainJob = launch {
                appRepository.flow().collect {
                    apps = it
                }
            }
        }
    }

    fun unregister() {
        if (isRegistered) {
            isRegistered = false
            stop()
            mainJob?.cancel()
            mainJob = null
        }
    }


    fun start() {
        if (!running && isRegistered) {
            running = true

            currentJob = launch {

                watcher.bandWithFlow.collect {

                    val currentTime = System.currentTimeMillis()

                    if (lastTime < currentTime - (DateUtils.MILLIS_PER_SECOND * 10)) {
                        val start = lastTime
                        lastTime = currentTime
                        registerLollipopTraffic(rxBytesLatest, txBytesLatest, currentTime)
                        registerTraffic(start)

                        rxBytesLatest = 0
                        txBytesLatest = 0

                        Log.i(TAG, "Traffic registered rx: ${it.first} tx: ${it.second}")
                    }else {
                        rxBytesLatest += it.first
                        txBytesLatest += it.second
                    }
                }
            }
        } else {
            Log.w(TAG, "TrafficRegistration already registered")
        }
    }

    fun stop() {
        running = false
        currentJob?.cancel()
        currentJob = null
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
                    network = if (isLTE()) {
                        Networks.NETWORK_4G
                    } else {
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
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

    companion object {
        const val TAG = "TrafficRegistration"
        private var rxBytesLatest = 0L
        private var txBytesLatest = 0L
    }
}