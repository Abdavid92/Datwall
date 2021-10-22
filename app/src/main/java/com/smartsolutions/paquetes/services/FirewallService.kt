package com.smartsolutions.paquetes.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.abdavid92.vpncore.*
import com.abdavid92.vpncore.socket.IProtectSocket
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.PacketManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.DatagramSocket
import java.net.Socket
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val TAG = "FirewallService"

/**
 * Servicio del cortafuegos.
 * */
@AndroidEntryPoint
class FirewallService : VpnService(), IProtectSocket, IObserverPacket, CoroutineScope {

    private var currentAppJob: Job? = null
    private var dynamicJob: Job? = null
    private var observeJob: Job? = null

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val dataStore = internalDataStore

    /**
     * Conexión del vpn
     * */
    private lateinit var vpnConnection: IVpnConnection

    /**
     * Hilo de la conexión vpn
     * */
    private var vpnConnectionThread: Thread? = null

    private var lastApp: String? = null

    /**
     * Repositorio de aplicaciones
     * */
    @Inject
    lateinit var appRepository: IAppRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var watcher: RxWatcher

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            /*Si la acción es ACTION_STOP, detengo el servicio.
            * Esto se hace así porque no se puede detener un servicio en primer
            * plano desde el contexto.*/
            if (it.action == ACTION_STOP_FIREWALL_SERVICE) {

                stopService()

                return START_NOT_STICKY
            } else if (it.action == ACTION_ALLOW_APP) {

                it.getParcelableExtra<App>(EXTRA_APP)?.let { app ->
                    app.tempAccess = true
                    launch(Dispatchers.IO) {
                        appRepository.update(app)
                    }

                    cancelAskNotification(app.uid)
                }

                if (vpnConnection.isConnected)
                    return START_STICKY

                return START_NOT_STICKY
            }
        }

        launchNotification()

        //Configuración extra del vpn
        vpnConnection = BasicVpnConnection(this)
            .setSessionName(getString(R.string.app_name))
            .setPendingIntent(getLaunchPendingIntent())

        vpnConnection.subscribe(this)

        if (vpnConnection is TrackerVpnConnection) {
            (vpnConnection as TrackerVpnConnection).allowUnknownUid(true)
        }

        vpnConnectionThread = Thread(vpnConnection)

        registerFlows()

        return START_STICKY
    }

    override fun observe(packet: Packet) {
        PacketManager.getInstance()
            .sendPacket(packet)
    }

    private fun registerFlows() {
        observeAppList()
        observeForegroundApp()
    }

    private fun observeAppList() {
        if (observeJob == null) {
            observeJob = launch(Dispatchers.IO) {
                appRepository.flow().collect {

                    vpnConnection.setAllowedPackageNames(it.filter { app ->
                        app.access || app.tempAccess
                    }.map { transformApp ->
                        return@map transformApp.packageName
                    }.toTypedArray())

                    //Inicio el vpn
                    if (!vpnConnection.isConnected)
                        vpnConnectionThread?.start()
                }
            }
        }
    }

    private fun observeForegroundApp() {

        var dynamicMode = true

        if (dynamicJob == null) {
            dynamicJob = launch {
                dataStore.data.collect {
                    dynamicMode = it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] ?: dynamicMode
                }
            }
        }

        if (currentAppJob == null) {
            currentAppJob = launch {
                watcher.currentAppFlow.collect {
                    //Aplicación en primer plano
                    val foregroundApp = it.first
                    //Aplicación que dejó el primer plano
                    val delayApp = it.second

                    if (dynamicMode &&
                        foregroundApp.packageName != lastApp &&
                        foregroundApp.packageName != packageName
                    ) {

                        lastApp = foregroundApp.packageName

                        //Si la aplicación tiene acceso en primer plano
                        if (foregroundApp.foregroundAccess) {
                            //Concedo acceso temporal y actualizo en el repostorio
                            foregroundApp.tempAccess = true
                            appRepository.update(foregroundApp)

                        /* Pero si no tiene acceso, es ejecutable, tiene permiso de acceso a internet
                         * y se puede preguntar por ella
                         * */
                        } else if (
                            !foregroundApp.access &&
                            foregroundApp.executable &&
                            foregroundApp.internet &&
                            foregroundApp.ask
                        ) {
                            //Lanzo la notificación con la aplicación
                            launchAskNotification(foregroundApp)
                        } else {
                            Log.i(
                                TAG,
                                "observeForegroundApp: Empty action for ${foregroundApp.packageName}"
                            )
                        }
                    }

                    delayApp?.let { app ->

                        /* Si la aplicación que dejó el primer plano tenía
                         * acceso temporal se lo quito y actualiza el repositorio.
                         * Esto hara que el vpn aplique los nuevos cambios automáticamente.*/
                        if (app.tempAccess) {

                            app.tempAccess = false

                            appRepository.update(app)

                            Log.i(TAG, "The application ${app.packageName} left the foreground")
                        }

                        cancelAskNotification(app.uid)
                    }
                }
            }
        }
    }

    private fun launchAskNotification(app: App) {

        val intent = Intent(this, FirewallService::class.java)
            .setAction(ACTION_ALLOW_APP)
            .putExtra(EXTRA_APP, app)

        val pendingIntent = PendingIntent.getService(
            this,
            0,
            intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )

        val notification = NotificationCompat.Builder(this, NotificationHelper.ALERT_CHANNEL_ID)
            .addAction(NotificationCompat.Action.Builder(
                R.drawable.ic_done,
                getString(R.string.btn_allow_text),
                pendingIntent
            ).build())
            .setContentTitle(app.name)
            .setContentText(getString(R.string.allow_app_text))
            .setSmallIcon(R.drawable.ic_firewall)
            .setStyle(NotificationCompat.BigTextStyle())
            .setAutoCancel(true)

        NotificationManagerCompat.from(this)
            .notify(
                app.uid,
                notification.build()
            )
    }

    private fun cancelAskNotification(uid: Int) {
        NotificationManagerCompat.from(this)
            .cancel(uid)
    }

    /**
     * Detiene el vpn y el servicio
     * */
    private fun stopService() {

        //Detengo el vpn
        vpnConnection.shutdown()
        vpnConnection.unsubscribe(this)
        vpnConnectionThread?.interrupt()
        job.cancel()

        //Detengo el servicio en primer plano
        stopForeground(false)
        stopSelf()
    }

    /**
     * Crea la notificación persistente del servicio.
     * */
    private fun launchNotification() {
        startForeground(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            NotificationCompat.Builder(this, NotificationHelper.MAIN_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_main_notification)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.firewall_service_running))
                .setContentIntent(getLaunchPendingIntent())
                .build()
        )
    }

    override fun onRevoke() {
        val notificationManager = ContextCompat
            .getSystemService(this, NotificationManager::class.java)

        val notification = NotificationCompat.Builder(this, NotificationHelper.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_main_notification)
            .setContentTitle(getString(R.string.firewall_force_stop))
            .setContentText(getString(R.string.firewall_force_stop_summary))
            .setStyle(NotificationCompat.BigTextStyle())
            .setContentIntent(getLaunchPendingIntent())

        notificationManager?.notify(NotificationHelper.ALERT_NOTIFICATION_ID, notification.build())

        stopService()
    }

    override fun protectSocket(socket: Socket) {
        this.protect(socket)
    }

    override fun protectSocket(socket: Int) {
        this.protect(socket)
    }

    override fun protectSocket(socket: DatagramSocket) {
        this.protect(socket)
    }

    override fun onDestroy() {
        super.onDestroy()

        if (vpnConnectionThread?.isInterrupted == false)
            vpnConnectionThread?.interrupt()

        vpnConnectionThread = null

        observeJob?.cancel()
        observeJob = null
        dynamicJob?.cancel()
        dynamicJob = null
        currentAppJob?.cancel()
        currentAppJob = null

        job.cancel()
    }

    private fun getLaunchPendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            this,
            FIREWALL_SERVICE_REQUEST_CODE,
            Intent(this, SplashActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
    }

    companion object {
        /**
         * Request code que se usa para el PendingIntent del vpn.
         * */
        const val FIREWALL_SERVICE_REQUEST_CODE = 932

        /**
         * Acción que se usa para detener el servicio.
         * */
        const val ACTION_STOP_FIREWALL_SERVICE = "com.smartsolutions.paquetes.action.STOP_FIREWALL_SERVICE"

        /**
         * Acción que se usa para permitir el acceso a una aplicación.
         * */
        const val ACTION_ALLOW_APP = "com.smartsolutions.paquetes.action.ALLOW_APP"

        /**
         * Aplicación.
         * */
        const val EXTRA_APP = "com.smartsolutions.paquetes.extra.APP"
    }
}