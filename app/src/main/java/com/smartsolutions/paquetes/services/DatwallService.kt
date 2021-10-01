package com.smartsolutions.paquetes.services

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import androidx.core.app.NotificationManagerCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToLong

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

    private val mNotificationManager by lazy {
        NotificationManagerCompat.from(this)
    }

    private lateinit var mNotificationBuilder: NotificationBuilder

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mNotificationBuilder = CircularNotificationBuilder(
            this,
            NotificationHelper.MAIN_CHANNEL_ID
        )

        startForeground(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            mNotificationBuilder.build()
        )

        kernel.tryRestoreState()

        registerBandWithCollector()
        registerUserDataBytesCollector()
        registerNotificationChangesCollector()

        watcherThread = Thread(watcher)

        watcherThread.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {

            mNotificationManager.notify(
                NotificationHelper.MAIN_NOTIFICATION_ID,
                mNotificationBuilder.apply {
                    setOngoing(false)
                }.build()
            )

            stopForeground(true)
            stopSelf()

            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        launch {

            val userData = userDataBytesRepository
                .bySimId(simManager.getDefaultSim(SimDelegate.SimType.DATA).id)
                .filter { it.exists() }

            updateNotification(userData)
        }
    }

    private fun registerNotificationChangesCollector() {
        launch {
            dataStore.data.collect { preferences ->

                val notificationClass = preferences[PreferencesKeys.NOTIFICATION_CLASS] ?:
                LinearNotificationBuilder::class.java.canonicalName

                mNotificationBuilder = NotificationBuilder.newInstance(
                    notificationClass,
                    this@DatwallService,
                    NotificationHelper.MAIN_CHANNEL_ID
                )

                val userData = userDataBytesRepository
                    .bySimId(simManager.getDefaultSim(SimDelegate.SimType.DATA).id)
                    .filter { it.exists() }

                updateNotification(userData)
            }
        }
    }

    /**
     * Registra un colector para la velocidad de ancho de banda de la red.
     * */
    private fun registerBandWithCollector() {
        launch {
            watcher.bandWithFlow.collect {
                updateBandWith(it.first, it.second)
            }
        }
    }

    /**
     * Registra un colector para el consumo de todos los tipos
     * de dataBytes de la linea predeterminada de datos.
     * */
    private fun registerUserDataBytesCollector() {
        launch {
            simManager.flowInstalledSims(false)
                .combine(userDataBytesRepository.flow()) { sims, userDataBytes ->
                    val defaultDataSim = sims.first { it.defaultData }

                    return@combine userDataBytes
                        .filter { it.simId == defaultDataSim.id }
                }
                .map {
                    return@map it.filter { dataBytes ->
                        dataBytes.exists()
                    }
                }
                .collect { userData ->
                    updateNotification(userData)
                }
        }
    }

    /**
     * Actualiza la notificación persistente con la lista de userDataBytes dado.
     *
     * @param userData - Lista de [UserDataBytes] que se usará para actualizar
     * los valores de la notificación.
     * */
    private fun updateNotification(userData: List<UserDataBytes>) {
        mNotificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            mNotificationBuilder.setNotificationData(userData)
                .build()
        )
    }

    /**
     * Actualiza el ícono de velocidad de ancho de banda. Este
     * método suma los bytes de descarga y los bytes de subida
     * para mostrarlos juntos.
     *
     * @param rxBytes - Bytes de descarga.
     * @param txBytes - Bytes de subida.
     * */
    private fun updateBandWith(rxBytes: Long, txBytes: Long) {
        mNotificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            mNotificationBuilder.setSmallIcon(getIcon(rxBytes + txBytes))
                .build()
        )
    }

    /**
     * Obtiene el ícono de la notificación usando los bytes dados.
     *
     * @param totalBytes - Bytes que se usarán para buscar el ícono correcto.
     * */
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

    override fun onDestroy() {
        job.cancel()

        watcher.stop()

        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "com.smartsolutions.paquetes.ACTION_STOP"
    }
}