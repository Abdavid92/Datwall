package com.smartsolutions.paquetes.services

import android.app.PendingIntent
import android.content.*
import android.net.VpnService
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.datwall.PreferencesKeys
import com.smartsolutions.datwall.dataStore
import com.smartsolutions.datwall.firewall.VpnConnection
import com.smartsolutions.datwall.repositories.IAppRepository
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.watcher.Watcher
import com.smartsolutions.paquetes.MainActivity
import com.smartsolutions.paquetes.NotificationChannels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.firewall.AskActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Servicio del cortafuegos.
 * */
@AndroidEntryPoint
class FirewallService : VpnService() {

    private val TAG = "FirewallService"

    /**
     * Conexión del vpn
     * */
    @Inject
    lateinit var vpnConnection: VpnConnection

    /**
     * Repositorio de aplicaciones
     * */
    @Inject
    lateinit var appRepository: IAppRepository

    /**
     * Preferencias de la aplicación
     * */

    /**
     * Receptor de radiodifución del observador
     * */
    private val watcherReceiver = object : BroadcastReceiver(), CoroutineScope {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO

        override fun onReceive(context: Context?, intent: Intent?) {

            //Aplicación en primer plano
            val foregroundApp = intent?.getParcelableExtra<App>(Watcher.EXTRA_FOREGROUND_APP)
            //Aplicación que dejó el primer plano
            val delayApp = intent?.getParcelableExtra<App>(Watcher.EXTRA_DELAY_APP)

            foregroundApp?.let { app ->

                //Si la aplicación tiene acceso en primer plano
                if (app.foregroundAccess) {
                    launch {
                        //Concedo acceso temporal y actualiza en el repostorio
                        app.tempAccess = true
                        appRepository.update(app)
                    }
                /* Pero si no tiene acceso, es ejecutable, tiene permiso de acceso a internet
                 * y se puede preguntar por ella
                 * */
                } else if (!app.access && app.executable && app.internet && app.ask) {
                    //Lanzo el AskActivity con la aplicación
                    val askIntent = Intent(context, AskActivity::class.java)
                        .putExtra(Watcher.EXTRA_FOREGROUND_APP, app)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    startActivity(askIntent)
                } else {
                    Log.i(TAG, "onReceive: Empty action for ${app.packageName}")
                }
            }

            delayApp?.let { app ->

                /*Si la aplicación que dejó el primer plano tenía
                * acceso temporal se lo quito y actualiza el repositorio.
                * Esto hara que el vpn aplique los nuevos cambios automáticamente.*/
                if (app.tempAccess) {

                    app.tempAccess = false

                    launch {
                        appRepository.update(app)

                        Log.i(TAG, "The application ${app.packageName} left the foreground")
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        //Configuración extra del vpn
        vpnConnection.service = this
        vpnConnection.pendingIntent = PendingIntent.getActivity(
            this,
            FIREWALL_SERVICE_REQUEST_CODE,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        launchNotification()

        //Si el modo dinámico está activado
        dataStore.data.map {
            if (it[PreferencesKeys.DYNAMIC_FIREWALL_ON] == true) {
                val filter = IntentFilter(Watcher.ACTION_CHANGE_APP_FOREGROUND)

                //Registro del receptor
                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(watcherReceiver, filter)
            }
        }
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
        vpnConnection.start()

        return super.onStartCommand(intent, flags, startId)
    }

    /**
     * Detiene el vpn y el servicio
     * */
    private fun stopService() {

        //Detengo el vpn
        vpnConnection.stop()

        //Elimino el registro del receiver
        LocalBroadcastManager.getInstance(this)
            .unregisterReceiver(watcherReceiver)

        //Detengo el servicio en primer plano
        stopForeground(true)
        stopSelf()
    }

    /**
     * Crea la notificación persistente del servicio.
     * */
    private fun launchNotification() {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(this, NotificationChannels.MAIN_CHANNEL_ID)
        } else {
            NotificationCompat.Builder(this)
        }

        //TODO: Ícono temporal
        builder.setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.firewall_service_running))

        startForeground(NotificationChannels.MAIN_NOTIFICATION_ID, builder.build())
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