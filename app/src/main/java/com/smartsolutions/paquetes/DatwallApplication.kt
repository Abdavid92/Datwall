package com.smartsolutions.paquetes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.datwall.watcher.PackageMonitor
import com.smartsolutions.datwall.watcher.Watcher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Clase principal de la aplicación
 * */
@HiltAndroidApp
class DatwallApplication : Application() {

    /**
     * Observador
     * */
    @Inject
    lateinit var watcher: Watcher

    /**
     * Monitor de paquetes
     * */
    @Inject
    lateinit var packageMonitor: PackageMonitor

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            /*Fuerzo la sincronización de la base de datos para
            * garantizar la integridad de los datos*/
            packageMonitor.forceSynchronization {
                //Después de sembrar la base de datos, inicio el observador
                watcher.start()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()
    }

    /**
     * Crea los canales de las notificaciones solo en android 8 en adelante.
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannels() {
        ContextCompat.getSystemService(this, NotificationManager::class.java)?.let { notificationManager ->

            //Canal principal
            val mainChannel = NotificationChannel(
                NotificationChannels.MAIN_CHANNEL_ID,
                getString(R.string.main_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )

            notificationManager.createNotificationChannel(mainChannel)
        }
    }
}