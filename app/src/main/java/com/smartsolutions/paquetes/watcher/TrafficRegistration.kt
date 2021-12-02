package com.smartsolutions.paquetes.watcher

import android.app.usage.NetworkStats
import android.content.Context
import android.net.TrafficStats
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class TrafficRegistration @Inject constructor(
    @ApplicationContext
    private val context: Context,
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
        get() = Dispatchers.Default

    private var running = false

    private var currentJob: Job? = null

    private var mainJob: Job? = null


    /**
     * Tiempo de Inicio que se usará a partir de Android 6 con NetworkStatsManager para comenzar
     * a recopilar los datos
     */
    private var startTime = getStartTime()

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
                    val list = mutableListOf<App>()
                    list.addAll(it)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        list.addAll(addMissingApps())
                    }
                    apps = list
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

            Log.i(TAG, "Traffic's registration started")

            currentJob = launch {

                watcher.bandWithFlow.collect {

                    val currentTime = System.currentTimeMillis()

                    if (lastTime < currentTime - (DateUtils.MILLIS_PER_SECOND * 15)) {

                        verifyTimeStart()

                        val start = lastTime
                        lastTime = currentTime
                        registerLollipopTraffic(rxBytesLatest, txBytesLatest, currentTime)
                        registerTraffic(start)

                        rxBytesLatest = 0
                        txBytesLatest = 0
                    } else {
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

        Log.i(TAG, "Traffic's registration stopped")
    }

    /**
     * Se encarga de obtener el trafico segun la version de Android y catalogarlo segun el tipo de
     * app que sea, national, international o free
     */
    private suspend fun registerTraffic(start: Long) {
        val international = Traffic()
        val national = Traffic()
        val messaging = Traffic()

        getTrafficsToRegister(start).forEach { traffic ->
            apps.firstOrNull { it.uid == traffic.uid }?.let { app ->
                when (app.trafficType) {
                    TrafficType.International -> {
                        international += traffic
                    }
                    TrafficType.National -> {
                        national += traffic
                    }
                    TrafficType.Messaging -> {
                        messaging += traffic
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
            messaging.totalBytes.bytes,
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
            val traffics = mutableListOf<Traffic>()
            simManager.getDefaultSimBoth(SimDelegate.SimType.DATA)?.let { sim ->
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
            }
            traffics.toList()
        }

        traffics.forEach { traffic ->
            lastTraffics.firstOrNull { it.uid == traffic.uid && it.simId == traffic.simId }
                ?.let { lastTraffic ->
                    toRegister.add(traffic - lastTraffic)
                    lastTraffics.remove(lastTraffic)
                }
        }

        lastTraffics.addAll(traffics)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            trafficRepository.create(toRegister.filter { it.totalBytes.bytes > 0 })
        }

        return toRegister
    }

    fun verifyTimeStart() {
        if (System.currentTimeMillis() - startTime > DateUtils.MILLIS_PER_DAY * 2) {
            startTime = getStartTime()
            lastTraffics.clear()
        }
    }

    fun getStartTime(): Long {
        return System.currentTimeMillis() - (12 * DateUtils.MILLIS_PER_HOUR)
    }

    /**
     * Se encarga de obtener el trafico general y guardarlo en Lollipop.
     */
    private suspend fun registerLollipopTraffic(rx: Long, tx: Long, currentTime: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M && rx > 0 && tx > 0) {
            simManager.getDefaultSimBoth(SimDelegate.SimType.DATA)?.id?.let {
                val traffic = Traffic(
                    NetworkUsageManager.GENERAL_TRAFFIC_UID,
                    rx,
                    tx,
                    it
                ).apply {
                    startTime = currentTime - DateUtils.MILLIS_PER_SECOND
                    endTime = currentTime
                }
                trafficRepository.create(traffic)
            }
        }
    }

    private fun isLTE(): Boolean {
        val isLTE = networkUtils.getNetworkGeneration() == NetworkUtils.NetworkType.NETWORK_4G
        if (isLTE) {
            launch(Dispatchers.IO) {
                context.internalDataStore.edit {
                    it[PreferencesKeys.ENABLED_LTE] = true
                }
            }
        }
        return isLTE
    }


    @RequiresApi(Build.VERSION_CODES.M)
    private fun addMissingApps(): List<App> {
        return listOf(
            App(
                "android.removed.sytem",
                NetworkStats.Bucket.UID_REMOVED,
                "Aplicaciones Desintaladas",
                1,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                TrafficType.International,
                null,
                null,
                null
            ),
            App(
                "android.hostpot.sytem",
                NetworkStats.Bucket.UID_TETHERING,
                "Conexión Compartida",
                1,
                false,
                false,
                false,
                false,
                false,
                true,
                false,
                TrafficType.International,
                null,
                null,
                null
            )
        )
    }

    companion object {
        const val TAG = "TrafficRegistration"
        private var rxBytesLatest = 0L
        private var txBytesLatest = 0L
    }
}