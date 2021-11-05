package com.smartsolutions.paquetes.services

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.watcher.RxWatcher
import com.smartsolutions.paquetes.watcher.TrafficRegistration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
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

    private var bandWidthJob: Job? = null
    private var userDataByteJob: Job? = null
    private var dataStoreJob: Job? = null

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    private val uiHelper by uiHelper()

    private var defaultPercent = 5

    /**
     * Porcentaje en que se deben lanzar las notificaciones de advertencias de
     * agotamiento de datos. Contiene 5 elementos
     *
     * Índices:
     * 0 - International
     * 1 - International Lte
     * 2 - Promo bonus
     * 3 - National
     * 4 - Daily bag
     * */
    private val percents = DataBytes.DataType.values()
        .map {
            it.ordinal
        }.toMutableList()
        .apply {
            for (i in this.indices) {
                this[i] = defaultPercent
            }
        }.toIntArray()

    private var showSecondaryNotifications = true

    private val notificationMetadata = mutableMapOf<DataBytes.DataType, Boolean>()

    @Inject
    lateinit var activationManager: IActivationManager

    @Inject
    lateinit var userDataBytesRepository: IUserDataBytesRepository

    @Inject
    lateinit var simManager: ISimManager

    @Inject
    lateinit var watcher: RxWatcher

    @Inject
    lateinit var trafficRegistration: TrafficRegistration

    private val mNotificationManager by lazy {
        NotificationManagerCompat.from(this)
    }

    private lateinit var mNotificationBuilder: NotificationBuilder

    override fun onBind(intent: Intent): IBinder {
        return DatwallBinder(this)
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

        Log.i(TAG, "Starting service")

        mNotificationBuilder = CircularNotificationBuilder(
            this,
            NotificationHelper.MAIN_CHANNEL_ID
        )

        startForeground(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            mNotificationBuilder.build()
        )

        Log.i(TAG, "registering band with collector")
        registerBandWithCollector()

        Log.i(TAG, "registering user data bytes collector")
        registerUserDataBytesCollector()

        Log.i(TAG, "registering data store collector")
        dataStoreCollector()

        Log.i(TAG, "starting watcher")
        watcher.start()

        Log.i(TAG, "registering traffic registration")
        trafficRegistration.register()

        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        launch {

            simManager.getDefaultSim(SimDelegate.SimType.DATA)?.id?.let {
                val userData = userDataBytesRepository
                    .bySimId(it)
                    .filter { it.exists() }

                updateNotification(userData)
            }
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()

        Log.i(TAG, "Me estoy quedando sin memoria")
    }

    private fun dataStoreCollector() {
        if (dataStoreJob != null)
            return

        dataStoreJob = launch(Dispatchers.IO) {
            settingsDataStore.data.collect { preferences ->

                val notificationClass = preferences[PreferencesKeys.NOTIFICATION_CLASS] ?:
                NotificationBuilder.DEFAULT_NOTIFICATION_IMPL

                if (mNotificationBuilder.javaClass.name != notificationClass) {
                    mNotificationBuilder = NotificationBuilder.newInstance(
                        notificationClass,
                        this@DatwallService,
                        NotificationHelper.MAIN_CHANNEL_ID
                    )

                    simManager.getDefaultSim(SimDelegate.SimType.DATA)?.id?.let {
                        val userData = userDataBytesRepository
                            .bySimId(it)
                            .filter { it.exists() }

                        updateNotification(userData)
                    }
                }

                percents[0] = preferences[PreferencesKeys.INTERNATIONAL_NOTIFICATION] ?:
                defaultPercent

                percents[1] = preferences[PreferencesKeys.INTERNATIONAL_LTE_NOTIFICATION] ?:
                defaultPercent

                percents[2] = preferences[PreferencesKeys.PROMO_BONUS_NOTIFICATION] ?:
                defaultPercent

                percents[3] = preferences[PreferencesKeys.NATIONAL_NOTIFICATION] ?:
                defaultPercent

                percents[4] = preferences[PreferencesKeys.DAILY_BAG_NOTIFICATION] ?:
                defaultPercent

                showSecondaryNotifications = preferences[PreferencesKeys.SHOW_SECONDARY_NOTIFICATIONS] ?: true
            }
        }
    }

    /**
     * Registra un colector para la velocidad de ancho de banda de la red.
     * */
    private fun registerBandWithCollector() {
        if (bandWidthJob != null)
            return

        bandWidthJob = launch(Dispatchers.IO) {

            watcher.bandWithFlow.collect {

                if (activationManager.canWork().first)
                    updateBandWith(it.first, it.second)
            }
        }
    }

    /**
     * Registra un colector para el consumo de todos los tipos
     * de dataBytes de la linea predeterminada de datos.
     * */
    private fun registerUserDataBytesCollector() {
        if (userDataByteJob != null)
            return

        userDataByteJob = launch {
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
                    sendDataNotifications(userData)
                }
        }
    }

    private fun sendDataNotifications(userData: List<UserDataBytes>) {
        if (showSecondaryNotifications) {

            userData.forEach { userDataBytes ->
                //Este es el porcentaje establecido para lanzar la notificación
                val percent = percents[userDataBytes.type.ordinal]

                /* Si el porcentaje configurado es cero significa que no se debe lanzar
                 * la notificación.*/
                if (percent > 0) {

                    //Porcentaje para saber exactamente por donde va el consumo
                    val userPercent =
                        (100f * userDataBytes.bytes / userDataBytes.initialBytes).toInt()

                    /* Si el porcentaje del userDataBytes es igual al porcentaje
                     * configurado se considera lanzar la notificación.*/
                    if (userPercent == percent) {

                        /* Si los metadatos de la notificación son falsos significa que no se ha
                         * lanzado todavía.*/
                        if (notificationMetadata[userDataBytes.type] != true) {
                            val notification = NotificationCompat.Builder(
                                this,
                                NotificationHelper.ALERT_CHANNEL_ID
                            ).setSmallIcon(R.drawable.splash_screen)
                                .setContentTitle(getString(R.string.low_data_notification))
                                .setContentIntent(
                                    NotificationBuilder.getSplashActivityPendingIntent(
                                        this
                                    )
                                )
                                .setStyle(
                                    NotificationCompat.BigTextStyle()
                                        .bigText(
                                            getString(
                                                R.string.low_data_notification_text,
                                                NotificationBuilder.getDataTitle(userDataBytes.type),
                                                percent
                                            )
                                        )
                                )
                                .build()

                            mNotificationManager.notify(
                                NotificationHelper.ALERT_NOTIFICATION_ID,
                                notification
                            )
                        }

                        /* Establezco los metadatos en true para saber que ya se lanzó la
                         * notificación. Esto será así mientras los porcentajes coincidan.
                         * Así se evita lanzar la notificación varias veces.*/
                        notificationMetadata[userDataBytes.type] = true
                    } else {
                        /*Cuando los porcentajes no coinciden establezco los metadatos en false
                         * para volver la lanzar la notificación cuando vuelvan a coincidir.*/
                        notificationMetadata[userDataBytes.type] = false
                    }
                }
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

        return uiHelper.getResource(name) ?: R.drawable.ic_main_notification
    }

    fun launchExpiredNotification() {
        val notification = NotificationCompat.Builder(this, NotificationHelper.MAIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_main_notification)
            .setContentTitle(getString(R.string.expired_try_period))
            .setContentText(getString(R.string.expired_try_period_summary))
            .setContentIntent(NotificationBuilder.getSplashActivityPendingIntent(this))

        NotificationManagerCompat.from(this)
            .notify(
                NotificationHelper.MAIN_NOTIFICATION_ID,
                notification.build()
            )
    }

    override fun onDestroy() {
        job.cancel()
        bandWidthJob?.cancel()
        userDataByteJob?.cancel()
        dataStoreJob?.cancel()


        watcher.stop()

        trafficRegistration.stop()
        trafficRegistration.unregister()

        Log.i(TAG, "Service was destroyed")
    }

    class DatwallBinder(
        private val service: DatwallService
    ) : Binder(), CoroutineScope {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO

        fun startTrafficRegistration() {
            launch {

                val canWork = service.activationManager.canWork()

                if (!canWork.first) {
                    if (canWork.second == IActivationManager.ApplicationStatuses.TrialPeriod) {
                        service.launchExpiredNotification()
                    }
                } else {
                    service.trafficRegistration.start()
                }
            }
        }

        fun stopTrafficRegistration() {
            service.trafficRegistration.stop()
        }
    }

    companion object {

        private const val TAG = "DatwallService"

        const val ACTION_STOP = "com.smartsolutions.paquetes.ACTION_STOP"
    }
}