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
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.PermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import com.smartsolutions.paquetes.services.BubbleFloatingService
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.services.FirewallService
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.ui.PresentationActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.activation.ActivationActivity
import com.smartsolutions.paquetes.ui.permissions.PermissionsActivity
import com.smartsolutions.paquetes.ui.setup.SetupActivity
import com.smartsolutions.paquetes.watcher.ChangeNetworkCallback
import com.smartsolutions.paquetes.watcher.PackageMonitor
import com.smartsolutions.paquetes.watcher.Watcher
import com.smartsolutions.paquetes.workers.TrafficRegistration
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
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
    private val trafficRegistration: TrafficRegistration,
    private val networkUtil: NetworkUtil
) : IChangeNetworkHelper, CoroutineScope {



    private var updateApplicationStatusJob: Job? = null
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
    suspend fun mainInForeground(activity: Activity) {

        createNotificationChannels()

        when {
            isFirstTime() -> {
                openActivity(PresentationActivity::class.java)
            }
            /*missingSomePermission() -> {
                openPermissionsActivity()
            }
            !isActivate() -> {
                openActivationActivity()
            }
            missingSomeConfiguration() -> {
                openSetupActivity()
            }*/
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

    suspend fun mainInBackground() {

        if (isInForeground())
            return

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

    override fun setDataMobileStateOn() {
        (context as DatwallApplication).dataMobileOn = true

        if (firewallOn) {
            startFirewall()
        }
        trafficRegistration.startRegistration()

        if (networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G) {
            launch {
                context.dataStore.edit {
                    it[PreferencesKeys.ENABLED_LTE] = true
                }
            }
        }
    }

    override fun setDataMobileStateOff() {
        (context as DatwallApplication).dataMobileOn = false

        if (firewallOn) {
          stopFirewall()
        }
        trafficRegistration.stopRegistration()
    }

    /**
     * Indica si es la primera vez que se abre la aplicación.
     * */
    private suspend fun isFirstTime(): Boolean {
        val wasOpen = context.dataStore.data
            .firstOrNull()
            ?.get(PreferencesKeys.APP_WAS_OPEN) == true

        context.dataStore.edit {
            it[PreferencesKeys.APP_WAS_OPEN] = true
        }

        return !wasOpen
    }

    private suspend fun isActivate(): Boolean {
        val status = activationManager.canWork().second

        return status != IActivationManager.ApplicationStatuses.Discontinued &&
                status != IActivationManager.ApplicationStatuses.Unknown
    }

    private fun openActivationActivity() {
        openActivity(ActivationActivity::class.java)
    }

    /**
     * Indica si falta alguna configuración importante.
     * */
    private suspend fun missingSomeConfiguration(): Boolean {
        return configurationManager.getIncompletedConfigurations()
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

        trafficRegistration.stopRegistration()
        stopBubbleFloating()
        stopFirewall()

    }


    fun startFirewall(){
        val permission = permissionManager.findPermission(IPermissionsManager.VPN_CODE)
        if (permission?.checkPermission?.invoke(permission, context) == true) {
            try {
                context.startService(Intent(context, FirewallService::class.java))
            }catch (e: Exception){

            }
        }else {
            throw MissingPermissionException(IPermissionsManager.VPN_PERMISSION_KEY)
        }
    }


    fun startBubbleFloating(){
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

        if (isGranted) {
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


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO
}