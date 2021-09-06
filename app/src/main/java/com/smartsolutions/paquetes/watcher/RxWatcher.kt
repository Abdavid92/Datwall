package com.smartsolutions.paquetes.watcher

import android.os.Build
import android.util.Log
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import dagger.Lazy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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
     * Último nombre de paquete que se resolvió
     * */
    private var currentPackageName: String? = null

    var currentAppFlow: Flow<Pair<App, App?>> = MutableSharedFlow()
        private set

    /**
     * Trabajo principal que está corriendo
     * */
    private var mainJob: Job? = null

    /**
     * Enciende el Watcher
     * */
    fun start() {

        this.running = true

        Log.i(TAG, "Watcher is running")

        mainJob = launch {
            while (running && isActive) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    packageMonitor.get().synchronizeDatabase()

                getCurrentApp()


            }
        }
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
    }
}