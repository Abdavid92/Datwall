package com.smartsolutions.paquetes

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import com.smartsolutions.paquetes.helpers.LegacyConfigurationHelper
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import com.smartsolutions.paquetes.receivers.TrafficRegistrationReceiver
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.services.FirewallService
import com.smartsolutions.paquetes.ui.MainActivity
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
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

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
    private val watcher: Watcher,
    private val networkUtil: NetworkUtil,
    private val legacyConfiguration: LegacyConfigurationHelper,
    private val trafficRegistration: TrafficRegistrationReceiver
) : IChangeNetworkHelper, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private var updateApplicationStatusJob: Job? = null
    private val defaultDispatcher = Dispatchers.Default
    private var bubbleOn = false
    private var firewallOn = false

    init {
        launch {

            context.dataStore.data.collect {
                bubbleOn = it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] == true
                firewallOn = it[PreferencesKeys.ENABLED_FIREWALL] == true
            }
        }
    }

    /**
     * Función principal que maqueta e inicia todos los servicios de la aplicación
     * y la actividad principal.
     * */
    fun mainInForeground(activity: Activity) {
        setLegacyConfiguration()

        GlobalScope.launch(defaultDispatcher) {
            createNotificationChannels()

            when {
                missingSomePermission() -> {
                    openPermissionsActivity()
                }
                !isRegisteredAndValid() -> {
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

        setLegacyConfiguration()

        GlobalScope.launch(defaultDispatcher) {
            createNotificationChannels()

            when {
                missingSomePermission() -> {
                    notify(
                        context.getString(R.string.missing_permmissions_title_notification),
                        context.getString(R.string.missing_permmissions_description_notification)
                    )
                }
                !isRegisteredAndValid() -> {
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

    /**
     * Establece las configuraciones del cortafuegos y la burbuja flotante
     * usando la versión anterior.
     *
     * Este método se eliminará en versiones posteriores.
     * */
    @Deprecated("Se eliminará en próximas versiones")
    private fun setLegacyConfiguration() {
        if (!legacyConfiguration.isConfigurationRestored()) {
            legacyConfiguration.setFirewallLegacyConfiguration()
            legacyConfiguration.setBubbleFloatingLegacyConfiguration()
        }
    }

    /**
     * Se invoca cuando se encienden los datos móbiles.
     * */
    override fun setDataMobileStateOn() {
        (context as DatwallApplication).dataMobileOn = true

        trafficRegistration.register()

        launch {
            if (firewallOn) {
                startFirewall()
            }

            if (bubbleOn) {
                startBubbleFloating()
            }

            if (networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G) {
                context.dataStore.edit {
                    it[PreferencesKeys.ENABLED_LTE] = true
                }
            }
        }
    }

    /**
     * Se invoca cuendo se apagan los datos móbiles.
     * */
    override fun setDataMobileStateOff() {
        (context as DatwallApplication).dataMobileOn = false

        if (firewallOn) {
          stopFirewall()
        }

        if (bubbleOn) {
            stopBubbleFloating()
        }

        trafficRegistration.unregister()
    }

    /**
     * Indica si la aplicación ya está registrada en el servidor y
     * no está obsoleta o descontinuada.
     * */
    private suspend fun isRegisteredAndValid(): Boolean {
        val status = activationManager.canWork().second

        return status != IActivationManager.ApplicationStatuses.Discontinued &&
                status != IActivationManager.ApplicationStatuses.Unknown &&
                status != IActivationManager.ApplicationStatuses.Deprecated &&
                status != IActivationManager.ApplicationStatuses.TooMuchOld
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
        /* En apis 22 o menor se registra un receiver para escuchar los cambios de redes.
         **/
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

            if (!changeNetworkReceiver.get().isRegister) {
                changeNetworkReceiver.get().register(context, filter)
            }
        } else {
            /* Si el sdk es api 23 o mayor se registra un callback de tipo
             * NetworkCallback en el ConnectivityManager para escuchar los cambios de redes.
             **/
            if (!changeNetworkCallback.get().isRegistered) {
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
            packageMonitor.forceSynchronization()

            //Después de sembrar la base de datos, inicio el observador
            watcher.start()
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

    /**
     * Detiene todos los servicios y trabajos de la aplicación.
     * */
    fun stopAllDatwall(){
        if (watcher.isActive){
            watcher.stop()
        }

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {

            if (changeNetworkReceiver.get().isRegister) {
                changeNetworkReceiver.get().unregister(context)
            }
        } else {
            if (changeNetworkCallback.get().isRegistered) {
                ContextCompat.getSystemService(context, ConnectivityManager::class.java)?.let {
                    changeNetworkCallback.get().unregister(it)
                }
            }
        }

        updateManager.cancelUpdateApplicationStatusWorker()

        context.stopService(Intent(context, DatwallService::class.java))

        trafficRegistration.unregister()
        stopBubbleFloating()
        stopFirewall()
    }

    private suspend fun startFirewall() {
        if (activationManager.canWork().first) {
            val permission = permissionManager.findPermission(IPermissionsManager.VPN_CODE)
            if (permission?.checkPermission?.invoke(permission, context) == true) {
                try {
                    context.startService(Intent(context, FirewallService::class.java))
                } catch (e: Exception) {

                }
            } else {
                throw MissingPermissionException(IPermissionsManager.VPN_PERMISSION_KEY)
            }
        }
    }

    private suspend fun startBubbleFloating(){
        val isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permission = permissionManager.findPermission(IPermissionsManager.DRAW_OVERLAYS_CODE)
            if (permission?.checkPermission?.invoke(permission, context) == true){
                true
            }else {
                throw MissingPermissionException(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            }
        } else {
            true
        }

        if (isGranted && activationManager.canWork().first) {
            try {
                context.startService(Intent(context, BubbleFloatingService::class.java))
            }catch (e: Exception){

            }
        }
    }

    fun stopBubbleFloating(turnOf: Boolean = false){
        if (turnOf){
            launch {
                context.dataStore.edit {
                    it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = false
                }
            }
        }
        try {
            context.stopService(Intent(context, BubbleFloatingService::class.java))
        }catch (e: Exception){

        }
    }

    fun stopFirewall(turnOf: Boolean = false){
        if (turnOf){
            launch {
                context.dataStore.edit {
                    it[PreferencesKeys.ENABLED_FIREWALL] = false
                }
            }
        }

        try {
            context.startService(
                Intent(context, FirewallService::class.java)
                    .setAction(FirewallService.ACTION_STOP_FIREWALL_SERVICE)
            )
        }catch (e: Exception) {

        }
    }

    private fun openActivity(activity: Class<out Activity>) {
        ContextCompat.startActivity(
            context,
            Intent(context, activity)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            null
        )
    }

    fun isInForeground(): Boolean {
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
                        Intent(context, SplashActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        0
                    )
                )
            }.build()
        )
    }
}