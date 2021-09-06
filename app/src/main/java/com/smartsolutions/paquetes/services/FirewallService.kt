package com.smartsolutions.paquetes.services

import android.app.PendingIntent
import android.content.*
import android.net.VpnService
import android.util.Log
import com.abdavid92.vpncore.*
import com.abdavid92.vpncore.socket.IProtectSocket
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.PacketManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.ui.MainActivity
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

/**
 * Servicio del cortafuegos.
 * */
@AndroidEntryPoint
class FirewallService : VpnService(), IProtectSocket, IObserverPacket, CoroutineScope {

    private val TAG = "FirewallService"

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /**
     * Conexión del vpn
     * */
    private lateinit var vpnConnection: IVpnConnection

    /**
     * Hilo de la conexión vpn
     * */
    private var vpnConnectionThread: Thread? = null

    private var enabledDynamicFirewall = false

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

        //Configuración extra del vpn
        vpnConnection = BasicVpnConnection(this)
            .setSessionName(getString(R.string.app_name))
            .setPendingIntent(PendingIntent.getActivity(
                this,
                FIREWALL_SERVICE_REQUEST_CODE,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            ))

        vpnConnection.subscribe(this)

        if (vpnConnection is TrackerVpnConnection) {
            (vpnConnection as TrackerVpnConnection).allowUnknownUid(true)
        }

        vpnConnectionThread = Thread(vpnConnection)

        launchNotification()
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

        //Inicio el vpn
        if (!vpnConnection.isConnected)
            vpnConnectionThread?.start()

        return super.onStartCommand(intent, flags, startId)
    }

    override fun observe(packet: Packet) {
        PacketManager.getInstance()
            .sendPacket(packet)
    }

    private fun registerFlows() {
        launch {
            dataStore.data.collect {
                enabledDynamicFirewall = it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] ?: false
            }
        }

        observeAppList()
        observeForegroundApp()
    }

    private fun observeAppList() {
        launch {
            appRepository.flow().collect {
                vpnConnection.setAllowedPackageNames(it.filter { app ->
                    app.access || app.tempAccess
                }.map { transformApp ->
                    return@map transformApp.packageName
                }.toTypedArray())
            }
        }
    }

    private fun observeForegroundApp() {
        launch {
            watcher.currentAppFlow.collect {
                //Aplicación en primer plano
                val foregroundApp = it.first
                //Aplicación que dejó el primer plano
                val delayApp = it.second

                //Si la aplicación tiene acceso en primer plano
                if (foregroundApp.foregroundAccess) {
                    //Concedo acceso temporal y actualiza en el repostorio
                    foregroundApp.tempAccess = true
                    appRepository.update(foregroundApp)

                /* Pero si no tiene acceso, es ejecutable, tiene permiso de acceso a internet
                 * y se puede preguntar por ella
                 * */
                } else if (
                    !foregroundApp.access &&
                    foregroundApp.executable &&
                    foregroundApp.internet &&
                    foregroundApp.ask) {
                    //Lanzo el AskActivity con la aplicación
                    val askIntent = Intent(this@FirewallService, AskActivity::class.java)
                        .putExtra(AskActivity.EXTRA_FOREGROUND_APP, foregroundApp)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    startActivity(askIntent)
                } else {
                    Log.i(TAG, "observeForegroundApp: Empty action for ${foregroundApp.packageName}")
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
        stopForeground(true)
        stopSelf()
    }

    /**
     * Crea la notificación persistente del servicio.
     * */
    private fun launchNotification() {
        startForeground(NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationHelper.buildNotification(NotificationHelper.MAIN_CHANNEL_ID).apply {
                //TODO: Ícono temporal
                setSmallIcon(R.mipmap.ic_launcher_round)
                setContentTitle(getString(R.string.app_name))
                setContentText(getString(R.string.firewall_service_running))
        }.build())
    }

    override fun onRevoke() {
        stopService()
        //TODO: Notificar que se murió el vpn.
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