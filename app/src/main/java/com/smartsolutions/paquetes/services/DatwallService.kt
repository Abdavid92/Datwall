package com.smartsolutions.paquetes.services

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
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

    @Inject
    lateinit var userDataBytesRepository: IUserDataBytesRepository

    @Inject
    lateinit var simManager: ISimManager

    @Inject
    lateinit var kernel: DatwallKernel

    @Inject
    lateinit var watcher: RxWatcher

    @Inject
    lateinit var gson: Gson

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

        mNotificationBuilder = LinearNotificationBuilder(
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
        dataStoreCollector()

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

    private fun dataStoreCollector() {
        launch {
            dataStore.data.collect { preferences ->

                val notificationClass = preferences[PreferencesKeys.NOTIFICATION_CLASS] ?:
                NotificationBuilder.DEFAULT_NOTIFICATION_IMPL

                if (mNotificationBuilder.javaClass.name != notificationClass) {
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

                showSecondaryNotifications = preferences[PreferencesKeys.SHOW_SECONDARY_NOTIFICATIONS] == true
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
                    sendDataNotifications(userData)
                }
        }
    }

    private fun sendDataNotifications(userData: List<UserDataBytes>) {
        if (showSecondaryNotifications) {

            userData.forEach { userDataBytes ->
                //Este es el porcentaje establecido para lanzar la notificación
                val percent = percents[userDataBytes.type.ordinal]

                /*Si el porcentaje configurado es cero significa que no se debe lanzar la notificación.*/
                if (percent > 0) {

                    /*Estos son los bytes guardados desde la última vez que se consideró lanzar
                    * una notificación*/
                    val metadata = getNotificationMetadata()[userDataBytes.type]

                    if (BuildConfig.DEBUG && metadata == null)
                        Log.i(
                            TAG,
                            "sendDataNotifications: metadata is null with ${userDataBytes.type}"
                        )

                    //Porcentaje con precisión decimal para saber exactamente por donde va el consumo
                    val userPercent =
                        100f * userDataBytes.bytes.toFloat() / userDataBytes.initialBytes.toFloat()

                    /* Si metadata es null significa que todavía no se ha lanzado ninguna notificación
                    * para este userDataBytes y si metadata es menor que los bytes significa que el userDataBytes
                    * ha sido reabastecido. En ese caso se debe considerar lanzar la notificación.
                    * Si el porcentaje del userDataBytes es exactamente igual al porcentaje
                    * configurado se lanza la notificación.*/
                    if (userPercent == percent.toFloat() && (metadata == null || metadata < userDataBytes.bytes)) {
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

                    setNotificationMetadata(userDataBytes.type, userDataBytes.bytes)
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

        return uiHelper.getResource(name) ?: R.mipmap.ic_launcher_foreground
    }

    private fun setNotificationMetadata(type: DataBytes.DataType, value: Long) {
        val metadata = getNotificationMetadata()

        metadata[type] = value

        val file = File(cacheDir, "notification_metadata")

        if (!file.exists() && !file.createNewFile())
            throw IOException("Can not create notification_metadata file")

        runCatching {
            val json = gson.toJson(metadata)

            val output = FileOutputStream(file)

            output.write(json.toByteArray())

            output.flush()
            output.close()
        }
    }

    /**
     * Metadatos de las notificaciones que se han lanzado que se usan
     * para decidir si se deben volver a lanzar.
     * */
    private fun getNotificationMetadata(): MutableMap<DataBytes.DataType, Long> {
        val file = File(cacheDir, "notification_metadata")

        if (!file.exists())
            return mutableMapOf()

        try {
            val json = FileInputStream(file)
                .bufferedReader()
                .readText()

            val typeToken = object : TypeToken<MutableMap<DataBytes.DataType, Long>>() {}.type

            return gson.fromJson(json, typeToken)
        } catch (e: Exception) {

        }

        return mutableMapOf()
    }

    override fun onDestroy() {
        job.cancel()

        watcher.stop()

        super.onDestroy()
    }

    companion object {

        private const val TAG = "DatwallService"

        const val ACTION_STOP = "com.smartsolutions.paquetes.ACTION_STOP"
    }
}