package com.smartsolutions.paquetes

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.watcher.ChangeNetworkCallback
import com.smartsolutions.paquetes.watcher.PackageMonitor
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.Lazy
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Clase principal de la aplicación. Contiene el inyector y se
 * encarga de iniciar los observadores, servicios, registrar los
 * callbacks y sembrar la base de datos.
 * */
@HiltAndroidApp
class DatwallApplication : Application(), Configuration.Provider {

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


    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Indica si los datos móbiles están encendidos.
     * */
    var dataMobileOn = false

    /**
     * Flows que registran el cambio de estado del servicio de accesibilidad.
     * Indica si el servicio está encendido.
     * */
    private val _uiScannerServiceEnabledFlow = MutableSharedFlow<Boolean>()
    val uiScannerServiceEnabledFlow = _uiScannerServiceEnabledFlow.asSharedFlow()
    var uiScannerServiceEnabled = false
        set(value) {
            field = value
            GlobalScope.launch {
                _uiScannerServiceEnabledFlow.emit(field)
            }
        }
    /**
     * Indica si el servicio está listo para trabajar.
     * */
    private val _uiScannerServiceReadyFlow = MutableSharedFlow<Boolean>()
    val uiScannerServiceReadyFlow = _uiScannerServiceReadyFlow.asSharedFlow()
    var uiScannerServiceReady = false
        set(value) {
            field = value
            GlobalScope.launch {
                _uiScannerServiceReadyFlow.emit(field)
            }
        }

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
            notificationHelper.createNotificationChannels()

        //Registro los callbacks
        registerCallbacks()
    }


    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

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
}