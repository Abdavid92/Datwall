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
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.ACTION_OPEN_FRAGMENT
import com.smartsolutions.paquetes.ui.EXTRA_FRAGMENT
import com.smartsolutions.paquetes.ui.FRAGMENT_UPDATE_DIALOG
import com.smartsolutions.paquetes.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)


    fun buildNotification(channelId: String): NotificationCompat.Builder {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, channelId)
        } else {
            NotificationCompat.Builder(context)
        }

        return builder
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
                                action = ACTION_OPEN_FRAGMENT
                                putExtra(EXTRA_FRAGMENT, FRAGMENT_UPDATE_DIALOG)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT
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
        )

        mainChannel.setShowBadge(false)
        mainChannel.enableVibration(false)

        notificationManager.createNotificationChannel(mainChannel)

        //Canal de alertas y mensajes
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            context.getString(R.string.alert_notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )

        notificationManager.createNotificationChannel(alertChannel)
    }


    /**
     * Constantes que contienen datos sobre los canales de notificaciones
     * */
   companion object NotificationChannels {

        //Canal principal
        const val MAIN_CHANNEL_ID = "main_channel"
        const val MAIN_NOTIFICATION_ID = 1
        const val ALERT_NOTIFICATION_ID = 2

        //Canal de alertas
        const val ALERT_CHANNEL_ID = "alert_channel"
    }

}