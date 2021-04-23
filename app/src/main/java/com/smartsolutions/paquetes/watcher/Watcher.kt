package com.smartsolutions.paquetes.watcher

import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Watcher (Observador) que estará vigilando la aplicación en primer plano.
 * Se encarga también de mantener actualizada la base de datos con los cambios de las aplicaciones
 * solo cuando el sistema es Android 8 en adelante.
 * */
@Singleton
class Watcher @Inject constructor(
    private val packageMonitor: PackageMonitor,
    private val watcherUtils: WatcherUtils,
    private val appRepository: IAppRepository,
    private val localBroadcastManager: LocalBroadcastManager
) : Runnable, AutoCloseable, CoroutineScope {

    private val TAG = "Watcher"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

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
     * Hilo en que se va a hacer el trabajo de observación
     * */
    private val thread = Thread(this)

    /**
     * Trabajo actual que está corriendo
     * */
    private var currentJob: Job? = null

    /**
     * Enciende el Watcher
     * */
    fun start() {

        this.running = true

        thread.start()

        Log.i(TAG, "Watcher is running")
    }

    override fun run() {

        while (running && !Thread.interrupted()) {

            try {

                currentJob = launch {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        packageMonitor.synchronizeDatabase()
                    }

                    watcherUtils.getLastApp()?.let {
                        launchBroadcasts(it)
                    }
                }

                Log.i(TAG, "sending ticktock broadcast")

                localBroadcastManager.sendBroadcast(Intent(ACTION_TICKTOCK))

                Thread.sleep(1000)
            } catch (e: Exception) {
                Log.i(TAG, "run: ${e.message}", e)
            }
        }
    }

    private suspend fun launchBroadcasts(packageName: String) {
        if (currentPackageName != packageName) {

            appRepository.get(packageName)?.let { app ->

                Log.i(TAG, "The application in foreground is ${app.packageName}")

                val intent = Intent(ACTION_CHANGE_APP_FOREGROUND)
                    .putExtra(EXTRA_FOREGROUND_APP, app)

                currentPackageName?.let {
                    appRepository.get(it)?.let { app ->

                        Log.i(TAG, "The application was delay foreground is ${app.packageName}")

                        intent.putExtra(EXTRA_DELAY_APP, app)
                    }
                }

                localBroadcastManager.sendBroadcast(intent)

                currentPackageName = packageName
            }
        }
    }

    /**
     * Detiene el Watcher
     * */
    fun stop() {
        this.running = false
        thread.interrupt()
        currentJob?.cancel()

        Log.i(TAG, "Watcher was stopped")
    }

    override fun close() {
        stop()
    }

    companion object {
        /**
         * Broadcast que se lanza cuando hay un cambio de aplicación
         * en primer plano.
         * */
        const val ACTION_CHANGE_APP_FOREGROUND = "com.smartsolutions.datwall.action.CHANGE_APP_FOREGROUND"

        /**
         * Broadcast que se lanza cada un segundo.
         * */
        const val ACTION_TICKTOCK = "com.smartsolutions.datwall.action.TICKTOCK"

        /**
         * Extra que contiene la aplicación que entró en el primer plano.
         * */
        const val EXTRA_FOREGROUND_APP = "com.smartsolutions.datwall.extra.FOREGROUND_APP"

        /**
         * Extra que contiene la aplicación que dejó el primer plano.
         * */
        const val EXTRA_DELAY_APP = "com.smartsolutions.datwall.extra.DELAY_APP"
    }
}