package com.smartsolutions.paquetes.services

import android.annotation.SuppressLint
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
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.watcher.Watcher
import com.smartsolutions.paquetes.workers.TrafficRegistration
import com.smartsolutions.paquetes.workers.TrafficRegistration2
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.lang.NullPointerException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

@AndroidEntryPoint
class DatwallService : Service(), CoroutineScope {

    private val binder = DatwallServiceBinder()

    @Inject
    lateinit var userDataBytesRepository: IUserDataBytesRepository

    @Inject
    lateinit var simManager: ISimManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var uiHelper: UIHelper

    private lateinit var notificationBuilder: Notification.Builder
    private lateinit var notificationManager: NotificationManager

    private val trafficRegistrationBroadcastReceiver = TrafficRegistrationBroadcastReceiver()
    private lateinit var contentView: RemoteViews
    private var userDataBytesInfo: List<UserDataBytes> = emptyList()



    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class DatwallServiceBinder: Binder() {
        val service: DatwallService
            get() = this@DatwallService
    }


    override fun onCreate() {
        super.onCreate()
        launch {
            userDataBytesRepository.flowBySimId(simManager.getDefaultDataSim().id).collect { userData ->
                userDataBytesInfo = userData.filter { it.exists() }
            }
        }
        notificationManager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: throw NullPointerException()

        notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NotificationHelper.MAIN_CHANNEL_ID)
        }else {
            Notification.Builder(this)
        }
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



        trafficRegistrationBroadcastReceiver.registerBroadcast(this)

        return START_STICKY
    }


    private fun updateView(){
        contentView = RemoteViews(this.packageName, R.layout.datwall_service_notification_normal)
    }


    private fun updateNotification(rxBytes: Long, txBytes: Long) {
        notificationBuilder.setSmallIcon(getIcon( rxBytes + txBytes))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.style = Notification.DecoratedCustomViewStyle()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            notificationBuilder.setCustomContentView(contentView)
            notificationBuilder.setCustomBigContentView(contentView)
        }else {
            notificationBuilder.setContent(contentView)
        }
        notificationManager.notify(NotificationHelper.MAIN_NOTIFICATION_ID, notificationBuilder.build())
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



    inner class TrafficRegistrationBroadcastReceiver: BroadcastReceiver() {

        var isRegistered = false

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent?.action == TrafficRegistration2.ACTION_TRAFFIC_REGISTRATION){
                val tx = intent.getLongExtra(TrafficRegistration.EXTRA_TRAFFIC_TX, 0L)
                val rx = intent.getLongExtra(TrafficRegistration.EXTRA_TRAFFIC_RX, 0L)
                updateView()
                updateNotification(rx, tx)
            }

        }


        fun registerBroadcast(context: Context) {
            if (!isRegistered) {
                LocalBroadcastManager.getInstance(context).registerReceiver(
                    this,
                    IntentFilter(TrafficRegistration2.ACTION_TRAFFIC_REGISTRATION)
                )
                isRegistered = true
            }
        }

        fun unregisterBroadcast(context: Context) {
            LocalBroadcastManager.getInstance(context)
                .unregisterReceiver(this)
        }

    }


    override fun onDestroy() {
        super.onDestroy()
        trafficRegistrationBroadcastReceiver.unregisterBroadcast(this)
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO


}