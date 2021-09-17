package com.smartsolutions.paquetes.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong
import kotlin.random.Random

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
    lateinit var kernel: DatwallKernel

    @Inject
    lateinit var watcher: RxWatcher

    private lateinit var watcherThread: Thread

    private val remoteViews: RemoteViews by lazy {
        RemoteViews(packageName, R.layout.datwall_service_notification_normal)
    }

    private val expandedRemoteViews by lazy {
        RemoteViews(packageName, R.layout.datwall_service_notification_expanded)
    }

    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }

    private val notificationBuilder by lazy {
        return@lazy NotificationCompat
            .Builder(this, NotificationHelper.MAIN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(expandedRemoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(PendingIntent
                .getActivity(
                    this,
                    0,
                    Intent(this, SplashActivity::class.java)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                ))
            .setOngoing(true)
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        setTextColor()

        runCatching {
            startForeground(
                NotificationHelper.MAIN_NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }

        kernel.tryRestoreState()

        registerBandWithFlow()
        beginUserDataBytesCollect()

        watcherThread = Thread(watcher)

        watcherThread.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopForeground(true)
            stopSelf()

            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun beginUserDataBytesCollect() {
        launch {
            simManager.flowInstalledSims(false)
                .combine(userDataBytesRepository.flow()) { sims, userDataBytes ->
                    val defaultDataSim = sims.first { it.defaultData }

                    return@combine userDataBytes
                        .filter { it.simId == defaultDataSim.id }
                }.collect { userData ->

                    userData.forEach { userDataBytes ->

                        if (userDataBytes.type == DataBytes.DataType.DailyBag) {
                            if (userDataBytes.exists() && !userDataBytes.isExpired()) {
                                remoteViews.setViewVisibility(R.id.daily_bag_layout, View.VISIBLE)
                                remoteViews.setViewVisibility(R.id.daily_bag_divider, View.VISIBLE)
                            } else {
                                remoteViews.setViewVisibility(R.id.daily_bag_layout, View.GONE)
                                remoteViews.setViewVisibility(R.id.daily_bag_divider, View.GONE)
                            }
                        }

                        val progressRef = R.id::class.java
                            .getDeclaredField("progress_${userDataBytes.type}")
                            .getInt(null)

                        val percentRef = R.id::class.java
                            .getDeclaredField("percent_${userDataBytes.type}")
                            .getInt(null)

                        if (userDataBytes.exists()) {
                            val percent = (100 * userDataBytes.bytes / userDataBytes.initialBytes)
                                .toInt()

                            remoteViews.setTextViewText(percentRef, if (userDataBytes.isExpired()) {
                                "exp"
                            } else {
                                "$percent%"
                            })

                            remoteViews.setProgressBar(
                                progressRef,
                                100,
                                percent,
                                false)
                        } else {
                            remoteViews.setTextViewText(percentRef, "n/a")
                            remoteViews.setProgressBar(
                                progressRef,
                                100,
                                0,
                                false
                            )
                        }
                    }

                    notificationManager.notify(
                        NotificationHelper.MAIN_NOTIFICATION_ID,
                        notificationBuilder.apply {
                            setCustomContentView(remoteViews)
                            setCustomBigContentView(expandedRemoteViews)
                        }.build()
                    )
                }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setTextColor()

        notificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationBuilder.apply {
                setCustomContentView(remoteViews)
                setCustomBigContentView(expandedRemoteViews)
            }.build()
        )
    }

    private fun setTextColor() {
        val methodName = "setTextColor"

        val color = if (uiHelper.isUIDarkTheme())
            Color.WHITE
        else
            Color.BLACK

        DataBytes.DataType.values().forEach {
            val percentRef = R.id::class.java
                .getDeclaredField("percent_${it.name}")
                .getInt(null)

            val labelRef = R.id::class.java
                .getDeclaredField("label_${it.name}")
                .getInt(null)

            remoteViews.setInt(percentRef, methodName, color)
            remoteViews.setInt(labelRef, methodName, color)
        }
    }

    private fun updateBandWith(rxBytes: Long, txBytes: Long) {
        notificationBuilder.setSmallIcon(getIcon( rxBytes + txBytes))
        notificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationBuilder.apply {
                setCustomContentView(this@DatwallService.remoteViews)
                setCustomBigContentView(this@DatwallService.expandedRemoteViews)
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
                if (traffic.value > 10.0) {
                    return R.drawable.traficc_10_more_mb
                } else {
                    val absolute = traffic.value.toInt()
                    val remainder = (traffic.value % 1 * 10).toInt()

                    "traficc_${absolute}_quot_${remainder}_mb"
                }
            }
            DataUnitBytes.DataUnit.GB -> {
                return R.drawable.traficc_10_more_mb
            }
        }

        return uiHelper.getResource(name) ?: R.mipmap.ic_launcher_foreground
    }

    private fun registerBandWithFlow() {
        launch {
            watcher.bandWithFlow.collect {
                updateBandWith(it.first, it.second)
            }
        }
    }

    override fun onDestroy() {
        job.cancel()

        watcher.stop()

        super.onDestroy()
    }

    inner class DatwallServiceBinder: Binder() {
        val service: DatwallService
            get() = this@DatwallService
    }

    companion object {
        const val ACTION_STOP = "com.smartsolutions.paquetes.ACTION_STOP"
    }
}