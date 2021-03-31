package com.smartsolutions.datwall.watcher

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.datwall.repositories.IAppRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Watcher (Observador) que estará vigilando la aplicación en primer plano.
 * Se encarga también de mantener actualizada la base de datos con los cambios de las aplicaciones
 * solo cuando el sistema es Android 8 en adelante.
 * */
class Watcher @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val packageMonitor: PackageMonitor,
    private val watcherUtils: WatcherUtils,
    private val appRepository: IAppRepository,
    private val localBroadcastManager: LocalBroadcastManager
) : Runnable, CoroutineScope {

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

                Thread.sleep(1000)
            } catch (e: Exception) {
                Log.i(TAG, "run: ${e.message}", e)
            }
        }
    }

    private suspend fun launchBroadcasts(packageName: String) {
        if (context.packageName != packageName) {

            appRepository.get(packageName)?.let { app ->
                if (app.executable || watcherUtils.isTheLauncher(packageName) && currentPackageName != packageName) {

                    val intent = Intent(ACTION_CHANGE_APP_FOREGROUND)
                        .putExtra(EXTRA_APP, app)

                    if (localBroadcastManager.sendBroadcast(intent))
                        Log.i(TAG, "launchBroadcasts: send broadcast success")
                    else
                        Log.i(TAG, "launchBroadcasts: fail sending broadcast")

                    currentPackageName?.let {
                        appRepository.get(it)?.let { delayApp ->

                            val delayIntent = Intent(ACTION_DELAY_APP_FOREGROUND)
                                .putExtra(EXTRA_APP, delayApp)

                            localBroadcastManager.sendBroadcast(delayIntent)

                        }
                    }
                }
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
    }

    companion object {
        /**
         * Broadcast que se lanza cuando hay un cambio de aplicación
         * en primer plano.
         * */
        const val ACTION_CHANGE_APP_FOREGROUND = "com.smartsolutions.datwall.action.CHANGE_APP_FOREGROUND"

        /**
         * Broadcast que se lanza junto con @see ACTION_CHANGE_APP_FOREGROUND
         * cuando una aplicación deja el primer plano.
         * */
        const val ACTION_DELAY_APP_FOREGROUND = "com.smartsolutions.datwall.action.DELAY_APP_FOREGROUND"

        /**
         * Extra que contiene la aplicación que entró o dejó el primer plano.
         * */
        const val EXTRA_APP = "com.smartsolutions.datwall.extra.APP"
    }
}