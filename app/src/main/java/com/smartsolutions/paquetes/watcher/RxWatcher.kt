package com.smartsolutions.paquetes.watcher

import android.net.TrafficStats
import android.os.Build
import android.util.Log
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import dagger.Lazy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class RxWatcher @Inject constructor(
    private val packageMonitor: Lazy<PackageMonitor>,
    private val watcherUtils: WatcherUtils,
    private val appRepository: IAppRepository
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /**
     * Indica si el Watcher está corriendo o no.
     * */
    var running = false
        private set

    /**
     * Trabajo principal que está corriendo
     * */
    private var mainJob: Job? = null

    /**
     * Último nombre de paquete que se resolvió
     * */
    private var currentPackageName: String? = null

    /**
     *
     * */
    val currentAppFlow: Flow<Pair<App, App?>> = MutableSharedFlow()

    /**
     *
     * */
    val bandWithFlow: Flow<Pair<Long, Long>> = MutableSharedFlow()

    /**
     * Enciende el Watcher
     * */
    fun start() {

        if (!running) {
            this.running = true

            Log.i(TAG, "Watcher is running")

            mainJob = launch {
                while (running && isActive) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        packageMonitor.get().synchronizeDatabase()

                    getCurrentApp()

                    getBandWith()

                    delay(1000)
                }
            }
        }
    }

    private suspend fun getBandWith() {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()

        if (rxBytes > 0 && txBytes > 0) {
            (bandWithFlow as MutableSharedFlow)
                .emit(rx - rxBytes to tx - txBytes)

            Log.i(TAG, "Band with rx: ${rx - rxBytes} tx: ${tx - txBytes}")
        }

        rxBytes = rx
        txBytes = tx
    }

    private suspend fun getCurrentApp() {
        watcherUtils.getLastApp()?.let { packageName ->
            if (currentPackageName != packageName) {

                appRepository.get(packageName)?.let { app ->

                    Log.i(TAG, "The application in foreground is ${app.packageName}")

                    val pair = app to if (currentPackageName != null)
                        appRepository.get(currentPackageName!!)
                    else
                        null

                    (currentAppFlow as MutableSharedFlow<Pair<App, App?>>)
                        .emit(pair)

                    currentPackageName = packageName
                }
            }
        }
    }

    /**
     * Detiene el Watcher
     * */
    fun stop() {
        this.running = false
        mainJob?.cancel()

        Log.i(TAG, "Watcher was stopped")
    }

    companion object {

        private const val TAG = "Watcher"

        @JvmStatic
        private var rxBytes = 0L

        @JvmStatic
        private var txBytes = 0L
    }
}