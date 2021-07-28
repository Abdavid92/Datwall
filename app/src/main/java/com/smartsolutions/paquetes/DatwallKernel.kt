package com.smartsolutions.paquetes

import android.content.Context
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
import com.smartsolutions.paquetes.watcher.ChangeNetworkCallback
import com.smartsolutions.paquetes.watcher.PackageMonitor
import com.smartsolutions.paquetes.watcher.Watcher
import com.smartsolutions.paquetes.workers.TrafficRegistration
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
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
    private val trafficRegistration: TrafficRegistration,
    private val notificationHelper: NotificationHelper,
    private val packageMonitor: PackageMonitor,
    private val watcher: Watcher
) {

    private var updateApplicationStatusJob: Job? = null

    fun main() {

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

    fun registerBroadcastsAndCallbacks() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

            changeNetworkReceiver.get().register(context, filter)
        } else {
            /* Si el sdk es api 23 o mayor se registra un callback de tipo
             * NetworkCallback en el ConnectivityManager para escuchar los cambios de redes.
             **/
            ContextCompat.getSystemService(context, ConnectivityManager::class.java)?.let {

                /*El Transport del request es de tipo cellular para escuchar los cambios de
                * redes móbiles solamente.*/
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

                it.registerNetworkCallback(request.build(), changeNetworkCallback.get())
            }
        }
    }

    fun registerWorkers() {
        trafficRegistration.startRegistration()

        updateApplicationStatusJob = GlobalScope.launch {
            context.dataStore.data.collect {
                val interval = it[PreferencesKeys.INTERVAL_UPDATE_SYNCHRONIZATION] ?: 24

                updateManager.scheduleUpdateApplicationStatusWorker(interval)
            }
        }
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            notificationHelper.createNotificationChannels()
    }

    fun synchronizeDatabaseAndStartWatcher() {
        GlobalScope.launch {
            /*Fuerzo la sincronización de la base de datos para
            * garantizar la integridad de los datos. Esto no sobrescribe
            * los valores de acceso existentes.*/
            packageMonitor.forceSynchronization {
                //Después de sembrar la base de datos, inicio el observador
                watcher.start()
            }
        }
    }
}