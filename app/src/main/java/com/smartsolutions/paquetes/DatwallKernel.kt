package com.smartsolutions.paquetes

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.ui.PresentationActivity
import com.smartsolutions.paquetes.watcher.ChangeNetworkCallback
import com.smartsolutions.paquetes.watcher.PackageMonitor
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class DatwallKernel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager,
    private val permissionManager: IPermissionsManager,
    private val updateManager: IUpdateManager,
    private val changeNetworkReceiver: Lazy<ChangeNetworkReceiver>,
    private val changeNetworkCallback: Lazy<ChangeNetworkCallback>,
    private val notificationHelper: NotificationHelper,
    private val packageMonitor: PackageMonitor,
    private val watcher: Watcher
) {

    private var updateApplicationStatusJob: Job? = null

    /**
     * Función principal que maqueta e inicia todos los servicios de la aplicación
     * y la actividad principal.
     * */
    suspend fun main() {
        if (isFirstTime()) {
            context.startActivity(Intent(context, PresentationActivity::class.java))
        } else {
            val missingPermissions = missingSomePermission()

            if (missingPermissions.isNotEmpty())
                requestPermissions(missingPermissions)
            else {
                createNotificationChannels()
                synchronizeDatabaseAndStartWatcher()
                registerBroadcastsAndCallbacks()
                registerWorkers()
                startServices()
                startMainActivity()
            }
        }
    }

    /**
     * Indica si es la primera vez que se abre la aplicación.
     * */
    suspend fun isFirstTime(): Boolean {
        return activationManager.getSavedDeviceApp() == null
    }

    /**
     * Indica si falta algún permiso.
     * */
    fun missingSomePermission(): List<Permission> {
        return permissionManager.getDeniedPermissions()
    }

    /**
     * Pide los permisos faltantes.
     * */
    fun requestPermissions(permissions: List<Permission>) {

    }

    /**
     * Registra los broadcasts y los callbacks.
     * */
    fun registerBroadcastsAndCallbacks() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

            if (!changeNetworkReceiver.get().isRegister) {
                changeNetworkReceiver.get().register(context, filter)
            }
        } else {
            if (!changeNetworkCallback.get().isRegistered) {
                /* Si el sdk es api 23 o mayor se registra un callback de tipo
                 * NetworkCallback en el ConnectivityManager para escuchar los cambios de redes.
                 **/
                ContextCompat.getSystemService(context, ConnectivityManager::class.java)?.let {
                    changeNetworkCallback.get().register(it)
                }
            }
        }
    }

    /**
     * Registra los workers.
     * */
    fun registerWorkers() {
        updateApplicationStatusJob = GlobalScope.launch(Dispatchers.Default) {
            if (!updateManager.wasScheduleUpdateApplicationStatusWorker()) {
                context.dataStore.data.collect {
                    val interval = it[PreferencesKeys.INTERVAL_UPDATE_SYNCHRONIZATION] ?: 24

                    updateManager.scheduleUpdateApplicationStatusWorker(interval)
                }
            }
        }
    }

    /**
     * Crea los canales de notificaciones.
     * */
    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !notificationHelper.areCreatedNotificationChannels()
        ) {
            notificationHelper.createNotificationChannels()
        }
    }

    /**
     * Sincroniza la base de datos y enciende el Watcher.
     * */
    fun synchronizeDatabaseAndStartWatcher() {
        if (!watcher.running) {
            GlobalScope.launch {
            /* Fuerzo la sincronización de la base de datos para
             * garantizar la integridad de los datos. Esto no sobrescribe
             * los valores de acceso existentes.*/
                packageMonitor.forceSynchronization {
                    //Después de sembrar la base de datos, inicio el observador
                    watcher.start()
                }
            }
        }
    }

    /**
     * Inicia los servicios.
     * */
    fun startServices() {
        context.startService(Intent(context, DatwallService::class.java))
        //TODO: Iniciar la burbuja flotante
    }

    /**
     * Inicia la actividad principal.
     * */
    fun startMainActivity() {
        ContextCompat.startActivity(
            context,
            Intent(context, MainActivity::class.java),
            null
        )
    }
}