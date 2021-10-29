package com.smartsolutions.paquetes.helpers

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

class NotificationHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)


    ///TODO: El ícono está sujeto a cambios
    fun buildNotification(channelId: String, icon: Int = R.drawable.ic_error): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, channelId).apply {
            setSmallIcon(icon)
        }
    }


    fun notify(id: Int, notification: Notification){
        notificationManager.notify(id, notification)
    }

    fun cancelNotification(id: Int){
        notificationManager.cancel(id)
    }


    fun notifyUpdate(title: String, text: String) {
        notify(
            ALERT_NOTIFICATION_ID,
            buildNotification(NotificationHelper.ALERT_CHANNEL_ID)
                .apply {
                    setSmallIcon(R.drawable.ic_update_notification)
                    setContentTitle(title)
                    setContentText(text)

                        setContentIntent(
                            PendingIntent.getActivity(
                                context,
                                123,
                                Intent(context, MainActivity::class.java).apply {
                                    action = MainActivity.ACTION_OPEN_FRAGMENT
                                    putExtra(MainActivity.EXTRA_FRAGMENT, MainActivity.FRAGMENT_UPDATE_DIALOG)
                                },
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                }else {
                                    PendingIntent.FLAG_UPDATE_CURRENT
                                }
                            )
                        )
                }.build()
        )
    }

    /**
     * Crea los canales de las notificaciones solo en android 8 en adelante.
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannels() {
        //Canal principal
        val mainChannel = NotificationChannel(
            MAIN_CHANNEL_ID,
            context.getString(R.string.main_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.main_description_notification_channel)
        }

        mainChannel.setShowBadge(false)
        mainChannel.enableVibration(false)

        notificationManager.createNotificationChannel(mainChannel)

        //Canal de alertas y mensajes
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            context.getString(R.string.alert_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.alert_description_notification_channel)
        }

        notificationManager.createNotificationChannel(alertChannel)


        //Canal de los workers
        val backgroundWorkChannel = NotificationChannel(
            WORKERS_CHANNEL_ID,
            context.getString(R.string.background_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.workers_description_notification_channel)
        }

        notificationManager.createNotificationChannel(backgroundWorkChannel)

        //Canal del firewall
        val firewallChannel = NotificationChannel(
            FIREWALL_CHANNEL_ID,
            context.getString(R.string.firewall_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            description = context.getString(R.string.firewall_description_notification_channel)
        }

        notificationManager.createNotificationChannel(firewallChannel)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun areCreatedNotificationChannels(): Boolean {
        return notificationManager.getNotificationChannel(MAIN_CHANNEL_ID) != null &&
                notificationManager.getNotificationChannel(ALERT_CHANNEL_ID) != null &&
                notificationManager.getNotificationChannel(WORKERS_CHANNEL_ID) != null &&
                notificationManager.getNotificationChannel(FIREWALL_CHANNEL_ID) != null
    }

    /**
     * Constantes que contienen datos sobre los canales de notificaciones
     * */
   companion object NotificationChannels {

        const val MAIN_NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2
        const val WORKERS_NOTIFICATION_ID = 3
        const val FIREWALL_NOTIFICATION_ID = 4

        //Canal principal
        const val MAIN_CHANNEL_ID = "main_channel"
        //Canal de alertas
        const val ALERT_CHANNEL_ID = "alert_channel"
        //Canal de Workers
        const val WORKERS_CHANNEL_ID = "workers_channel"
        //Canal del Firewall
        const val FIREWALL_CHANNEL_ID = "firewall_channel"
    }

}