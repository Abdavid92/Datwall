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

    private var watcherJob: Job? = null

    /**
     * Indica si el Watcher está corriendo o no.
     * */
    var running = false
        private set

    /**
     * Último nombre de paquete que se resolvió
     * */
    private var currentPackageName: String? = null

    /**
     * Provee la aplicación en primer plano y la que dejó el primer plano.
     * Contiene un [Pair] en el que el primer valor es la aplicación que
     * entró en primer plano y el segundo valor es la aplicación que dejó el
     * primer plano. No siempre esta última se podrá resolver, generalmente al
     * encender el Watcher.
     * */
    val currentAppFlow: Flow<Pair<App, App?>> = MutableSharedFlow()

    /**
     * Provee cada un segundo el ancho de banda de la red.
     * Contiene un [Pair] en el que el primer valor son los bytes descargados y
     * el segundo son los bytes subidos.
     * */
    val bandWithFlow: Flow<Pair<Long, Long>> = MutableSharedFlow()

    fun start() {
        Log.i(TAG, "Starting watcher")

        if (!running) {
            running = true

            Log.i(TAG, "Watcher started success")

            watcherJob = launch {

                while (this.isActive && running) {

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        launch(Dispatchers.IO) {
                            packageMonitor.get().synchronizeDatabase()
                        }
                    }

                    provideCurrentApp()

                    getBandWith()

                    delay(1000)
                }

                Log.i(TAG, "Watcher was stopped")
            }
        } else {
            Log.w(TAG, "Failed starting watcher. Was started")
        }
    }

    private suspend fun getBandWith() {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()

        if (rxBytes > 0 && txBytes > 0) {
            val download = rx - rxBytes
            val upload = tx - txBytes
            lastRxBytes = download
            lastTxBytes = upload
            lastBytes += download + upload

            if (download >= 0 && upload >= 0) {
                (bandWithFlow as MutableSharedFlow)
                    .emit(download to upload)

                Log.i(TAG, "Band with rx: $download tx: $upload")
            } else {
                (bandWithFlow as MutableSharedFlow)
                    .emit(0L to 0L)
            }
        }

        rxBytes = rx
        txBytes = tx
    }

    private fun provideCurrentApp() {
        watcherUtils.getLastApp()?.let { packageName ->
            if (currentPackageName != packageName) {

                launch {
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
    }

    /**
     * Detiene el Watcher
     * */
    fun stop() {
        Log.i(TAG, "Stopping watcher")

        this.running = false
        watcherJob?.cancel()
        watcherJob = null
    }

    companion object {

        private const val TAG = "RxWatcher"

        @JvmStatic
        private var rxBytes = 0L

        @JvmStatic
        private var txBytes = 0L

        var lastBytes = 0L
        var lastRxBytes = 0L
        var lastTxBytes = 0L
    }
}