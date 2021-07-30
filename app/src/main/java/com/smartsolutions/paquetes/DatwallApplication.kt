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

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /**
     * Indica si los datos móbiles están encendidos.
     * */
    var dataMobileOn = false

    /**
     * Indica si el servicio está encendido.
     * */
    var uiScannerServiceEnabled = false

    /**
     * Indica si el servicio está listo para trabajar.
     * */
    var uiScannerServiceReady = false

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}