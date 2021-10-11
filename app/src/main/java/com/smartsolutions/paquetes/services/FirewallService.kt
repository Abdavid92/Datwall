package com.smartsolutions.paquetes.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.abdavid92.vpncore.*
import com.abdavid92.vpncore.socket.IProtectSocket
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.PacketManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.firewall.AskActivity
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

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

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

    override fun onCreate() {
        super.onCreate()

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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            /*Si la acción es ACTION_STOP, detengo el servicio.
            * Esto se hace así porque no se puede detener un servicio en primer
            * plano desde el contexto.*/
            if (it.action == ACTION_STOP_FIREWALL_SERVICE) {

                stopService()

                return START_NOT_STICKY
            }
        }

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
        launch(Dispatchers.IO) {
            appRepository.flow().collect {

                //Inicio el vpn
                if (!vpnConnection.isConnected)
                    vpnConnectionThread?.start()

                vpnConnection.setAllowedPackageNames(it.filter { app ->
                    app.access || app.tempAccess
                }.map { transformApp ->
                    return@map transformApp.packageName
                }.toTypedArray())
            }
        }
    }

    private fun observeForegroundApp() {

        var dynamicMode = false

        launch {
            dataStore.data.collect {
                dynamicMode = it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] == true
            }
        }

        launch {
            watcher.currentAppFlow.collect {
                //Aplicación en primer plano
                val foregroundApp = it.first
                //Aplicación que dejó el primer plano
                val delayApp = it.second

                if (dynamicMode &&
                    foregroundApp.packageName != lastApp &&
                    foregroundApp.packageName != packageName) {

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
                        //Lanzo el AskActivity con la aplicación
                        val askIntent = Intent(this@FirewallService, AskActivity::class.java)
                            .putExtra(AskActivity.EXTRA_FOREGROUND_APP, foregroundApp)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        startActivity(askIntent)
                    } else {
                        Log.i(
                            TAG,
                            "observeForegroundApp: Empty action for ${foregroundApp.packageName}"
                        )
                    }
                }

                delayApp?.let { app ->

                    /*Si la aplicación que dejó el primer plano tenía
                    * acceso temporal se lo quito y actualiza el repositorio.
                    * Esto hara que el vpn aplique los nuevos cambios automáticamente.*/
                    if (app.tempAccess) {

                        app.tempAccess = false

                        appRepository.update(app)

                        Log.i(TAG, "The application ${app.packageName} left the foreground")
                    }
                }
            }
        }
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
    }
}