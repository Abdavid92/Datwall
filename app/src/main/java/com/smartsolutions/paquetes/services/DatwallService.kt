package com.smartsolutions.paquetes.services

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.receivers.TrafficRegistrationNewReceiver
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.lang.NullPointerException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

/**
 * Servicio principal de la aplicación. Mantiene abierta la aplicación
 * en fondo. Resuelve el ancho de banda de la conexión y actualiza el
 * estado de los datos cada cierto tiempo en la notificación.
 * */
@AndroidEntryPoint
class DatwallService : Service(), CoroutineScope {

    private val job = Job()

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val binder = DatwallServiceBinder()

    private val uiHelper by uiHelper()

    @Inject
    lateinit var userDataBytesRepository: IUserDataBytesRepository

    @Inject
    lateinit var simManager: ISimManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    //private lateinit var notificationBuilder: Notification.Builder
    //private lateinit var notificationManager: NotificationManager

    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private val trafficRegistrationBroadcastReceiver = TrafficRegistrationBroadcastReceiver()
    private lateinit var contentView: RemoteViews
    private var userDataBytesInfo: List<UserDataBytes> = emptyList()



    override fun onBind(intent: Intent): IBinder {
        return binder
    }


    override fun onCreate() {
        super.onCreate()

        notificationManager = NotificationManagerCompat.from(this)
        notificationBuilder = NotificationCompat
            .Builder(this, NotificationHelper.MAIN_CHANNEL_ID)

        /*launch {
            userDataBytesRepository.flow()
                .combine(simManager.flowInstalledSims(false)) { dataBytes, sims ->
                    val defaultDataSim = sims.first { it.defaultData }
                    return@combine dataBytes.filter { it.exists() && it.simId == defaultDataSim.id }
                }
                .collect { dataBytes ->
                    //TODO: Actualizar los valores
                }
            /*userDataBytesRepository.flowBySimId(simManager.getDefaultDataSim().id).collect { userData ->
                userDataBytesInfo = userData.filter { it.exists() }
            }*/
        }*/
        trafficRegistrationBroadcastReceiver.registerBroadcast(this)
        /*notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: throw NullPointerException()

        notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NotificationHelper.MAIN_CHANNEL_ID)
        }else {
            Notification.Builder(this)
        }*/
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        startForeground(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            //TODO cambiar icono de la notificacion
            notificationHelper.buildNotification(NotificationHelper.MAIN_CHANNEL_ID).apply {
                setContentTitle("Servicio Principal")
                setContentText("En funcionamiento")
            }.build()
        )

        return START_STICKY
    }


    private fun updateView(){
        contentView = RemoteViews(this.packageName, R.layout.datwall_service_notification_normal)
    }


    private fun updateNotification(rxBytes: Long, txBytes: Long) {
        notificationBuilder.setSmallIcon(getIcon( rxBytes + txBytes))
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.style = Notification.DecoratedCustomViewStyle()
        }*/
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setCustomContentView(contentView)
            notificationBuilder.setCustomBigContentView(contentView)
        }else {
            notificationBuilder.setContent(contentView)
        }*/
        notificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationBuilder.apply {
                setCustomContentView(this@DatwallService.contentView)
                setCustomBigContentView(this@DatwallService.contentView)
            }.build())
    }


    private fun getIcon(totalBytes: Long): Int {
        val traffic = DataUnitBytes(totalBytes).getValue()

        val name = when (traffic.dataUnit) {
            DataUnitBytes.DataUnit.B -> {
                return R.drawable.traficc_0_kb
            }
            DataUnitBytes.DataUnit.KB -> {
                if (traffic.value > 999) {
                    return R.drawable.traficc_1_quot_0_mb
                }
                "traficc_${traffic.value.roundToLong()}_kb"
            }
            DataUnitBytes.DataUnit.MB -> {
                val values = traffic.value.toString().split(".")
                "traficc_${values[0]}_quot_${values[1]}_mb"
            }
            else -> {
                return R.drawable.ic_bubble_notification
            }

        }

        return uiHelper.getResource(name) ?: R.drawable.ic_bubble_notification
    }

    override fun onDestroy() {
        super.onDestroy()
        trafficRegistrationBroadcastReceiver
            .unregisterBroadcast(this)
        job.cancel()
    }

    inner class DatwallServiceBinder: Binder() {
        val service: DatwallService
            get() = this@DatwallService
    }

    inner class TrafficRegistrationBroadcastReceiver: BroadcastReceiver() {

        var isRegistered = false

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == TrafficRegistrationNewReceiver.ACTION_TRAFFIC_REGISTRATION){
                val tx = intent.getLongExtra(TrafficRegistrationNewReceiver.EXTRA_TRAFFIC_TX, 0L)
                val rx = intent.getLongExtra(TrafficRegistrationNewReceiver.EXTRA_TRAFFIC_RX, 0L)
                updateView()
                updateNotification(rx, tx)
            }

        }


        fun registerBroadcast(context: Context) {
            if (!isRegistered) {
                LocalBroadcastManager.getInstance(context).registerReceiver(
                    this,
                    IntentFilter(TrafficRegistrationNewReceiver.ACTION_TRAFFIC_REGISTRATION)
                )
                isRegistered = true
            }
        }

        fun unregisterBroadcast(context: Context) {
            LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(this)
        }

    }
}