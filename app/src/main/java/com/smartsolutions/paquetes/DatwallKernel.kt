package com.smartsolutions.paquetes

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.FragmentContainerActivity
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
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
    private val watcher: Watcher,
    private val exceptionsController: ExceptionsController
) {

    private var updateApplicationStatusJob: Job? = null

    /**
     * Función principal que maqueta e inicia todos los servicios de la aplicación
     * y la actividad principal.
     * */
    @MainThread
    fun mainInForeground(activity: Activity) {

        registerExceptionsController()

        if (isFirstTime()) {
            context.startActivity(
                Intent(context, PresentationActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                    )
            )
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

    fun mainInBackground() {

    }

    /**
     * Indica si es la primera vez que se abre la aplicación.
     * */
    fun isFirstTime(): Boolean {
        return runBlocking {
            val wasOpen = context.dataStore.data
                .firstOrNull()
                ?.get(PreferencesKeys.APP_WAS_OPEN) == true

            context.dataStore.edit {
                it[PreferencesKeys.APP_WAS_OPEN] = true
            }

            return@runBlocking !wasOpen
        }
    }

    /**
     * Indica si falta alguna configuración importante.
     * */
    fun missingSomeConfiguration(): Boolean {

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
        val requestCodes = mutableListOf<Int>()

        permissions.forEach {
            requestCodes.add(it.requestCode)
        }

        val intent = Intent(context, FragmentContainerActivity::class.java).apply {
            action = FragmentContainerActivity.ACTION_OPEN_FRAGMENT
            putExtra(
                FragmentContainerActivity.EXTRA_FRAGMENT,
                FragmentContainerActivity.EXTRA_FRAGMENT_PERMISSIONS
            )
            putExtra(
                FragmentContainerActivity.EXTRA_PERMISSIONS_REQUESTS_CODES,
                requestCodes.toIntArray()
            )
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        }

        ContextCompat.startActivity(
            context,
            intent,
            null
        )
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
            Intent(context, MainActivity::class.java)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                ),
            null
        )
    }

    fun registerExceptionsController() {
        if (!exceptionsController.isRegistered)
            exceptionsController.register()
    }
}