package com.smartsolutions.paquetes

import android.app.Application
import android.content.res.Resources
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Clase principal de la aplicación. Contiene el inyector y se
 * encarga de iniciar los observadores, servicios, registrar los
 * callbacks y sembrar la base de datos.
 * */
@HiltAndroidApp
class DatwallApplication : Application(), Configuration.Provider, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var exceptionsController: ExceptionsController

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

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    var kernel: DatwallKernel? = null


    override fun onCreate() {
        super.onCreate()

        if (!exceptionsController.isRegistered) {
            exceptionsController.register()
        }

        val themeMode: Int

        runBlocking {
            val preferences = dataStore.data
                .firstOrNull()

            themeMode = preferences?.get(PreferencesKeys.THEME_MODE) ?:
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        if (themeMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(themeMode)
        }
    }



    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}