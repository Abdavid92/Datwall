package com.smartsolutions.paquetes

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.datwall.NotificationChannels
import com.smartsolutions.datwall.watcher.ChangeNetworkCallback
import com.smartsolutions.datwall.watcher.PackageMonitor
import com.smartsolutions.datwall.watcher.Watcher
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Clase principal de la aplicación. Contiene el inyector y se
 * encarga de iniciar los observadores, servicios, registrar los
 * callbacks y sembrar la base de datos.
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

    /**
     * Callback que contiene eventos que se llaman cuando hay un cambio de red.
     * Se registra solo en la api 23 en adelante. En api 22 y 21 se usa un receiver para
     * cumlir el mismo objetivo. Esta envuelto en una instancia de Lazy para no inyectarlo innecesariamente
     * cuando no se vaya a registrar.
     * */
    @Inject
    lateinit var changeNetworkCallback: Lazy<ChangeNetworkCallback>

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            /*Fuerzo la sincronización de la base de datos para
            * garantizar la integridad de los datos. Esto no sobrescribe
            * los valores de acceso existentes.*/
            packageMonitor.forceSynchronization {
                //Después de sembrar la base de datos, inicio el observador
                watcher.start()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannels()

        //Registro los callbacks
        registerCallbacks()
    }

    /**
     * Registra los callbacks de la aplicación.
     * */
    private fun registerCallbacks() {
        /* Si el sdk es api 23 o mayor se registra un callback de tipo
         * NetworkCallback en el ConnectivityManager para escuchar los cambios de redes.
         * */
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.getSystemService(this, ConnectivityManager::class.java)?.let {

                /*El Transport del request es de tipo cellular para escuchar los cambios de
                * redes móbiles solamente.*/
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

                it.registerNetworkCallback(request.build(), changeNetworkCallback.get())
            }
        }
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

            //Canal de alertas y mensajes
            val alertChannel = NotificationChannel(
                NotificationChannels.ALERT_CHANNEL_ID,
                getString(R.string.alert_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationManager.createNotificationChannel(alertChannel)
        }
    }
}