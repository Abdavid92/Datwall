package com.smartsolutions.paquetes

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.LifecycleOwner
import androidx.work.Configuration
import com.smartsolutions.paquetes.exceptions.ExceptionsController
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.RuntimeException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import android.os.StrictMode
import android.os.StrictMode.VmPolicy


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

    @Inject
    lateinit var preferencesRepository: IPreferencesRepository

    private var kernelRunning = false

    /**
     * Indica si el servicio de accesibilidad está listo para trabajar.
     * */
    var uiScannerServiceReady = false

    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork() // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        super.onCreate()

        if (!exceptionsController.isRegistered) {
            exceptionsController.register()
        }

        // Única lectura bloqueante: carga la copia en memoria de las preferencias antes de que
        // se abra cualquier pantalla, para que estas lean sin bloquear.
        val themeMode = runBlocking(Dispatchers.IO) {
            preferencesRepository.awaitLoaded()
        }.themeMode ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

        if (themeMode != AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) {
            AppCompatDelegate.setDefaultNightMode(themeMode)
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

        synchronizeDatabase()

        main()
    }

    fun main() {
        if (!kernelRunning) {
            // Antes de lanzar el kernel, para que ninguna pantalla siga el destino anterior.
            kernel.clearStartDestination()

            launch {
                kernelRunning = true

                kernel.main()

                kernelRunning = false
            }
        }
    }

    private fun synchronizeDatabase(){
        launch {
            if (kernel.canContinue(false)){
                kernel.synchronizeDatabase()
            }
        }
    }


    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

}