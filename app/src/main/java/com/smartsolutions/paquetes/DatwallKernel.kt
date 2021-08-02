package com.smartsolutions.paquetes

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.ui.PresentationActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.activation.ActivationActivity
import com.smartsolutions.paquetes.ui.permissions.PermissionsActivity
import com.smartsolutions.paquetes.ui.setup.SetupActivity
import com.smartsolutions.paquetes.watcher.ChangeNetworkCallback
import com.smartsolutions.paquetes.watcher.PackageMonitor
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatwallKernel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager,
    private val permissionManager: IPermissionsManager,
    private val configurationManager: IConfigurationManager,
    private val updateManager: IUpdateManager,
    private val changeNetworkReceiver: Lazy<ChangeNetworkReceiver>,
    private val changeNetworkCallback: Lazy<ChangeNetworkCallback>,
    private val notificationHelper: NotificationHelper,
    private val packageMonitor: PackageMonitor,
    private val watcher: Watcher
) : IChangeNetworkHelper {

    private var updateApplicationStatusJob: Job? = null

    private val defaultDispatcher = Dispatchers.Default

    /**
     * Función principal que maqueta e inicia todos los servicios de la aplicación
     * y la actividad principal.
     * */
    fun mainInForeground(activity: Activity) {
        GlobalScope.launch(defaultDispatcher) {
            createNotificationChannels()

            when {
                missingSomePermission() -> {
                    openPermissionsActivity()
                }
                !isActivate() -> {
                    openActivationActivity()
                }
                missingSomeConfiguration() -> {
                    openSetupActivity()
                }
                else -> {
                    synchronizeDatabaseAndStartWatcher()
                    registerBroadcastsAndCallbacks()
                    registerWorkers()
                    startServices()
                    startMainActivity()
                }
            }
            activity.finish()
        }
    }

    fun mainInBackground() {

        if (isInForeground())
            return

        GlobalScope.launch(defaultDispatcher) {
            createNotificationChannels()

            when {
                missingSomePermission() -> {
                    notify(
                        context.getString(R.string.missing_permmissions_title_notification),
                        context.getString(R.string.missing_permmissions_description_notification)
                    )
                }
                !isActivate() -> {
                    notify(
                        context.getString(R.string.generic_needed_action_title_notification),
                        context.getString(R.string.generic_needed_action_description_notification)
                    )
                }
                missingSomeConfiguration() -> {
                    notify(
                        context.getString(R.string.missing_configuration_title_notification),
                        context.getString(R.string.missing_configuration_description_notification)
                    )
                }
                else -> {
                    synchronizeDatabaseAndStartWatcher()
                    registerBroadcastsAndCallbacks()
                    registerWorkers()
                    startServices()
                }
            }
        }
    }

    override fun setDataMobileStateOn() {

    }

    override fun setDataMobileStateOff() {

    }

    /**
     * Indica si es la primera vez que se abre la aplicación.
     * */
    /*private suspend fun isFirstTime(): Boolean {
        val wasOpen = context.dataStore.data
            .firstOrNull()
            ?.get(PreferencesKeys.APP_WAS_OPEN) == true

        context.dataStore.edit {
            it[PreferencesKeys.APP_WAS_OPEN] = true
        }

        return !wasOpen
    }*/

    private suspend fun isActivate(): Boolean {
        val status = activationManager.canWork().second

        return status != IActivationManager.ApplicationStatuses.Discontinued &&
                status != IActivationManager.ApplicationStatuses.Unknown &&
                status != IActivationManager.ApplicationStatuses.Deprecated
    }

    private fun openActivationActivity() {
        openActivity(ActivationActivity::class.java)
    }

    /**
     * Indica si falta alguna configuración importante.
     * */
    private suspend fun missingSomeConfiguration(): Boolean {
        return configurationManager.getUncompletedConfigurations()
            .isNotEmpty()
    }

    private fun openSetupActivity() {
        openActivity(SetupActivity::class.java)
    }

    /**
     * Indica si falta algún permiso.
     * */
    private fun missingSomePermission(): Boolean {
        return permissionManager.getDeniedPermissions().isNotEmpty()
    }

    /**
     * Pide los permisos faltantes.
     * */
    private fun openPermissionsActivity() {
        openActivity(PermissionsActivity::class.java)
    }

    /**
     * Registra los broadcasts y los callbacks.
     * */
    private fun registerBroadcastsAndCallbacks() {
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
    private fun registerWorkers() {
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
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !notificationHelper.areCreatedNotificationChannels()
        ) {
            notificationHelper.createNotificationChannels()
        }
    }

    /**
     * Sincroniza la base de datos y enciende el Watcher.
     * */
    private suspend fun synchronizeDatabaseAndStartWatcher() {
        if (!watcher.running) {
            /* Fuerzo la sincronización de la base de datos para
             * garantizar la integridad de los datos. Esto no sobrescribe
             * los valores de acceso existentes.*/
            packageMonitor.forceSynchronization {
                //Después de sembrar la base de datos, inicio el observador
                watcher.start()
            }
        }
    }

    /**
     * Inicia los servicios.
     * */
    private fun startServices() {
        context.startService(Intent(context, DatwallService::class.java))
    }

    /**
     * Inicia la actividad principal.
     * */
    private fun startMainActivity() {
        openActivity(MainActivity::class.java)
    }

    private fun openActivity(activity: Class<out Activity>) {
        ContextCompat.startActivity(
            context,
            Intent(context, activity)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            null
        )
    }

    private fun isInForeground(): Boolean {
        val activityManager = ContextCompat.getSystemService(
            context,
            ActivityManager::class.java
        ) ?: return false

        return activityManager.runningAppProcesses.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == context.packageName
        }
    }

    private fun notify(title: String, description: String) {
        notificationHelper.notify(
            NotificationHelper.ALERT_NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.ALERT_CHANNEL_ID
            ).apply {
                setContentTitle(title)
                setContentText(description)
                setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        0,
                        Intent(context, SplashActivity::class.java),
                        0
                    )
                )
            }.build()
        )
    }
}