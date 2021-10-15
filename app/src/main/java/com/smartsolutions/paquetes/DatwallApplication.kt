package com.smartsolutions.paquetes

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.RuntimeException
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
        get() = Dispatchers.Default

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var exceptionsController: ExceptionsController

    @Inject
    lateinit var kernel: DatwallKernel

    /**
     * Indica si el servicio está encendido.
     * */
    var uiScannerServiceEnabled = false

    /**
     * Indica si el servicio está listo para trabajar.
     * */
    var uiScannerServiceReady = false

    override fun onCreate() {
        super.onCreate()

        if (!exceptionsController.isRegistered) {
            exceptionsController.register()
        }

        val themeMode: Int

        runBlocking {
            val preferences = settingsDataStore.data
                .firstOrNull()

            themeMode = preferences?.get(PreferencesKeys.THEME_MODE) ?:
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }

        if (themeMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(themeMode)
        }

        launch {
            kernel.main()
        }

        if (BuildConfig.DEBUG) {
            try {
                Class.forName("dalvik.system.CloseGuard")
                    .getMethod("setEnabled", Boolean::class.javaPrimitiveType)
                    .invoke(null, true)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException(e)
            }
        }
    }

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}