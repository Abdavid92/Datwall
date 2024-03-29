package com.smartsolutions.paquetes

import android.app.Activity
import android.app.ActivityManager
import android.app.PendingIntent
import android.content.*
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.managers.contracts.*
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import com.smartsolutions.paquetes.services.DatwallService
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.permissions.PermissionsActivity
import com.smartsolutions.paquetes.ui.setup.SetupActivity
import com.smartsolutions.paquetes.watcher.*
import com.smartsolutions.paquetes.workers.DropLogsWorker
import com.smartsolutions.paquetes.workers.TrafficDbOptimizer
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class DatwallKernel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val permissionManager: IPermissionsManager,
    private val configurationManager: IConfigurationManager,
    private val updateManager: IUpdateManager,
    private val dataPackageManager: IDataPackageManager,
    private val changeNetworkReceiver: Lazy<ChangeNetworkReceiver>,
    private val changeNetworkCallback: Lazy<ChangeNetworkCallback>,
    private val notificationHelper: NotificationHelper,
    private val packageMonitor: PackageMonitor,
    private val networkUtils: NetworkUtils,
    private val legacyConfiguration: LegacyConfigurationHelper,
    private val firewallHelper: FirewallHelper,
    private val bubbleServiceHelper: BubbleServiceHelper,
    private val synchronizationManager: ISynchronizationManager
) : IChangeNetworkHelper, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var datwallBinder: DatwallService.DatwallBinder? = null

    private val activityManager = ContextCompat
        .getSystemService(context, ActivityManager::class.java) ?: throw NullPointerException()

    private val mainServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            datwallBinder = service as DatwallService.DatwallBinder

            if (DATA_MOBILE_ON)
                datwallBinder?.startTrafficRegistration()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            datwallBinder = null
        }

    }


    private var openActivity: Pair<Class<out Activity>, Bundle?>? = null
    private val openActivitySubscribers =
        mutableMapOf<LifecycleOwner, (
            activity: Class<out Activity>,
            args: Bundle?,
            application: DatwallApplication
        ) -> Unit>()

    init {
        launch {

            context.internalDataStore.data.collect {
                BUBBLE_ON = it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] == true
                FIREWALL_ON = it[PreferencesKeys.ENABLED_FIREWALL] == true
            }
        }
    }

    /**
     * Función principal que maqueta e inicia todos los servicios de la aplicación.
     * */
    suspend fun main() {

        //Verifica que no se haya detenido la app debido a una excepcion. En ese caso detiene la ejecución
        if (!canContinue(true))
            return

        //Crea los canales de notificaciones
        createNotificationChannels()

        //Restablece la configuración de la versión anterior
        setLegacyConfiguration()

        //Crea o actualiza los paquetes de datos
        createOrUpdatePackages()

        when {
            //Verifica los permisos
            missingSomePermission() -> {
                openActivity(PermissionsActivity::class.java)
                considerNotify(
                    context.getString(R.string.missing_permmissions_title_notification),
                    context.getString(R.string.missing_permmissions_description_notification)
                )
            }
            //Verfica las configuraciones iniciales
            missingSomeConfiguration() -> {
                openActivity(
                    SetupActivity::class.java,
                    Bundle().apply {

                        val configurations = configurationManager.getUncompletedConfigurations()

                        putString(
                            SetupActivity.EXTRA_INITIAL_FRAGMENT,
                            configurations[0].fragment.name
                        )
                    }
                )
                considerNotify(
                    context.getString(R.string.missing_configuration_title_notification),
                    context.getString(R.string.missing_configuration_description_notification)
                )
            }
            else -> {
                openActivity(SplashActivity::class.java)
                //Inicia los servicios
                startMainService()
                //Registra los broadcasts y los callbacks
                registerBroadcastsAndCallbacks()
                //Registra los workers
                registerWorkers()
                //Inicia la actividad principal
                openActivity(MainActivity::class.java)
            }
        }
    }

    fun addOpenActivityListener(
        lifecycleOwner: LifecycleOwner,
        listener: (
            activity: Class<out Activity>,
            args: Bundle?,
            application: DatwallApplication
        ) -> Unit
    ) {

        if (lifecycleOwner.lifecycle.currentState != Lifecycle.State.DESTROYED) {
            openActivitySubscribers[lifecycleOwner] = listener

            if (openActivity != null && lifecycleOwner::class.java.name != openActivity!!.first.name)
                listener(
                    openActivity!!.first,
                    openActivity!!.second,
                    context.applicationContext as DatwallApplication
                )

            lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {

                override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                    if (event == Lifecycle.Event.ON_DESTROY) {
                        openActivitySubscribers.remove(lifecycleOwner)

                        lifecycleOwner.lifecycle.removeObserver(this)
                    }
                }
            })
        }
    }

    fun removeOpenActivityListener(key: LifecycleOwner) {
        openActivitySubscribers.remove(key)
    }

    private fun considerNotify(title: String, description: String) {
        if (!isInForeground()) {
            notify(title, description)
        }
    }

    private suspend fun createOrUpdatePackages() {
        withContext(Dispatchers.IO) {
            dataPackageManager.createOrUpdateDataPackages()
        }
    }

    /**
     * Establece las configuraciones del cortafuegos y la burbuja flotante
     * usando la versión anterior.
     *
     * Este método se eliminará en versiones posteriores.
     * */
    private fun setLegacyConfiguration() {
        launch {
            if (!legacyConfiguration.isConfigurationRestored()) {
                legacyConfiguration.setFirewallLegacyConfiguration()
                legacyConfiguration.setBubbleFloatingLegacyConfiguration()
                legacyConfiguration.setConfigurationRestored()
            }
        }
    }

    /**
     * Se invoca cuando se encienden los datos móbiles.
     * */
    override fun setDataMobileStateOn() {
        DATA_MOBILE_ON = true

        datwallBinder?.startTrafficRegistration()

        launch {
            if (FIREWALL_ON) {
                startFirewall()
            }

            if (BUBBLE_ON) {
                startBubbleFloating()
            }

            if (networkUtils.getNetworkGeneration() == NetworkUtils.NetworkType.NETWORK_4G) {
                context.internalDataStore.edit {
                    it[PreferencesKeys.ENABLED_LTE] = true
                }
            }
        }
    }

    /**
     * Se invoca cuando se apagan los datos móbiles.
     * */
    override fun setDataMobileStateOff() {
        DATA_MOBILE_ON = false

        datwallBinder?.stopTrafficRegistration()

        launch {
            if (FIREWALL_ON) {
                stopFirewall()
            }

            if (BUBBLE_ON) {
                stopBubbleFloating()
            }
        }
    }

    /**
     * Indica si falta alguna configuración importante.
     * */
    private suspend fun missingSomeConfiguration(): Boolean {
        return configurationManager.getUncompletedConfigurations().isNotEmpty()
    }

    /**
     * Indica si falta algún permiso.
     * */
    private fun missingSomePermission(): Boolean {
        return permissionManager.getDeniedPermissions().isNotEmpty()
    }

    /**
     * Registra los broadcasts y los callbacks.
     * */
    @Suppress("DEPRECATION")
    private fun registerBroadcastsAndCallbacks() {

        //En apis 22 o menor se registra un receiver para escuchar los cambios de redes.
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
        updateApplicationStatusJob = launch {
            context.workersDataStore.data.collect {
                val interval = it[PreferencesKeys.INTERVAL_UPDATE_SYNCHRONIZATION] ?: 6

                updateManager.scheduleUpdateApplicationStatusWorker(interval)
            }
        }

        dataSyncJob = launch {
            context.workersDataStore.data.collect {
                val enabled = it[PreferencesKeys.ENABLE_DATA_SYNCHRONIZATION] ?: true
                if (enabled)
                    synchronizationManager.scheduleUserDataBytesSynchronization(15)
                else
                    synchronizationManager.cancelScheduleUserDataBytesSynchronization()
            }
        }

        //Programación del worker que limpia los logs
        val workManager = WorkManager.getInstance(context)

        val tag = "drop_log_worker"

        workManager.cancelAllWorkByTag(tag)

        val request = PeriodicWorkRequestBuilder<DropLogsWorker>(
            3, TimeUnit.DAYS
        ).build()

        workManager.enqueue(request)

        TrafficDbOptimizer.registerWorkerIfNeeded(context, 48)
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
     * Sincroniza la base de datos.
     * */
    fun synchronizeDatabase() {
        /* Fuerzo la sincronización de la base de datos para
         * garantizar la integridad de los datos. Esto no sobrescribe
         * los valores de acceso existentes.*/
        packageMonitor.forceSynchronization()
    }

    /**
     * Inicia el servicio principal.
     * */
    private fun startMainService() {
        val datwallServiceIntent = Intent(context, DatwallService::class.java)

        ContextCompat.startForegroundService(context, datwallServiceIntent)

        context.bindService(datwallServiceIntent, mainServiceConnection, Context.BIND_AUTO_CREATE)
    }

    private suspend fun startFirewall() {
        firewallHelper.startFirewall(true)
    }

    private suspend fun startBubbleFloating() {
        bubbleServiceHelper.startBubble(false)
    }

    private suspend fun stopBubbleFloating() {
        bubbleServiceHelper.stopBubble()
    }

    private suspend fun stopFirewall(turnOf: Boolean = false) {
        if (turnOf) {
            firewallHelper.stopFirewall(true)
        } else {
            firewallHelper.stopFirewall(false)
        }
    }

    private suspend fun openActivity(activity: Class<out Activity>, args: Bundle? = null) {
        openActivity = activity to args

        withContext(Dispatchers.Main) {

            runCatching {
                openActivitySubscribers.forEach {
                    if (it.key.javaClass.name != activity.name)
                        it.value(
                            activity,
                            args,
                            context.applicationContext as DatwallApplication
                        )
                }
            }
        }
    }

    private fun isInForeground(): Boolean {
        return activityManager.runningAppProcesses?.any {
            it.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    it.processName == context.packageName
        } ?: false
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
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                )
            }.build()
        )
    }

    suspend fun canContinue(reset: Boolean): Boolean {
        //Verifica que no se haya detenido la app debido a una excepcion. En ese caso detiene la ejecución
        if (withContext(Dispatchers.IO) {
                val isThrowed = context.internalDataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.IS_THROWED) == true

                if (reset) {
                    context.internalDataStore.edit {
                        it[PreferencesKeys.IS_THROWED] = false
                    }
                }

                return@withContext isThrowed
            })
            return false

        return true
    }

    companion object {
        private var updateApplicationStatusJob: Job? = null
        private var dataSyncJob: Job? = null

        private var BUBBLE_ON = false
        private var FIREWALL_ON = false

        /**
         * Indica si los datos móbiles están encendidos.
         * */
        var DATA_MOBILE_ON = false
    }
}