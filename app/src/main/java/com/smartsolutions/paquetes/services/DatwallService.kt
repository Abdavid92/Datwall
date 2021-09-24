package com.smartsolutions.paquetes.services

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.ui.SplashActivity
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

    private var notificationEmpty = false

    @Inject
    lateinit var userDataBytesRepository: IUserDataBytesRepository

    @Inject
    lateinit var simManager: ISimManager

    @Inject
    lateinit var kernel: DatwallKernel

    @Inject
    lateinit var watcher: RxWatcher

    private lateinit var watcherThread: Thread

    private var remoteViews: RemoteViews? = null

    private var expandedRemoteViews: RemoteViews? = null

    private val notificationManager by lazy {
        NotificationManagerCompat.from(this)
    }

    private val notificationBuilder by lazy {
        return@lazy NotificationCompat
            .Builder(this, NotificationHelper.MAIN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setCustomContentView(remoteViews)
            .setCustomBigContentView(expandedRemoteViews)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContentIntent(PendingIntent
                .getActivity(
                    this,
                    0,
                    Intent(this, SplashActivity::class.java)
                        .setFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        ),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    } else {
                        PendingIntent.FLAG_UPDATE_CURRENT
                    }
                ))
            .setOngoing(true)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        setTheme(R.style.Theme_Datwall)

        runCatching {
            startForeground(
                NotificationHelper.MAIN_NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }

        kernel.tryRestoreState()

        registerBandWithCollector()
        registerUserDataBytesCollector()

        watcherThread = Thread(watcher)

        watcherThread.start()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            notificationBuilder.setOngoing(false)
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

            if (userData.isNotEmpty()) {
                notificationEmpty = false
                updateNotification(userData)
            } else {
                notificationEmpty = true
                setEmptyNotification()
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
                    if (userData.isNotEmpty()) {
                        notificationEmpty = false
                        updateNotification(userData)
                    } else {
                        notificationEmpty = true
                        setEmptyNotification()
                    }
                }
        }
    }

    /**
     * Establece una notificación con el texto de que no hay datos disponibles
     * para consumir.
     * */
    private fun setEmptyNotification() {
        notificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationBuilder.apply {
                setCustomContentView(null)
                setCustomBigContentView(null)
                setContentTitle(getString(R.string.empty_noti_title))
                setContentText(getString(R.string.empty_noti_text))
            }.build()
        )
    }

    /**
     * Actualiza la notificación persistente con la lista de userDataBytes dado.
     *
     * @param userData - Lista de [UserDataBytes] que se usará para actualizar
     * los valores de la notificación.
     * */
    private fun updateNotification(userData: List<UserDataBytes>) {
        //remoteViews.removeAllViews(R.id.content_view)
        //expandedRemoteViews.removeAllViews(R.id.content_view)
        remoteViews = RemoteViews(packageName, R.layout.datwall_service_notification)
        expandedRemoteViews = RemoteViews(packageName, R.layout.datwall_service_notification_expanded)

        for (i in userData.indices) {

            val title = getDataTitle(userData[i].type)

            addRemoteViewsContent(
                userData[i],
                title,
                i != 0
            )

            addExpandedRemoteViewContent(
                userData[i],
                title,
                i != 0
            )
        }

        setFirstExpiredDate(userData)

        val color = if (uiHelper.isUIDarkTheme())
            ContextCompat.getColor(this, R.color.background_dark)
        else
            ContextCompat.getColor(this, R.color.white)

        notificationManager.notify(
            NotificationHelper.MAIN_NOTIFICATION_ID,
            notificationBuilder.apply {
                setCustomContentView(remoteViews)
                setCustomBigContentView(expandedRemoteViews)
                setColor(color)
            }.build()
        )
    }

    /**
     * Obtiene un título legible a establecer en la notificación
     * usando el dataType dado.
     *
     * @param dataType - [DataBytes.DataType]
     *
     * @return [String] el título legible.
     * */
    private fun getDataTitle(dataType: DataBytes.DataType): String {
        return when (dataType) {
            DataBytes.DataType.International -> "Internacional"
            DataBytes.DataType.InternationalLte -> "Lte"
            DataBytes.DataType.PromoBonus -> "Promoción"
            DataBytes.DataType.National -> "Nacional"
            DataBytes.DataType.DailyBag -> "Bolsa diaria"
        }
    }

    /**
     * Establece la fecha de expiración del paquete más próximo a vencer
     * en la notificación expandida.
     *
     * @param userData
     * */
    private fun setFirstExpiredDate(userData: List<UserDataBytes>) {
        if (userData.isEmpty())
            return

        var data = userData[0]

        userData.forEach {
            if (data.expiredTime > it.expiredTime)
                data = it
            else if (data.expiredTime == it.expiredTime && data.priority < it.priority)
                data = it
        }

        val date = Date(data.expiredTime)

        val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())

        val dataTitle = getDataTitle(data.type)

        expandedRemoteViews?.setTextViewText(
            R.id.date_exp,
            getString(R.string.date_exp, dataTitle, dateFormat.format(date))
        )
        expandedRemoteViews?.setInt(
            R.id.date_exp,
            "setTextColor",
            if (uiHelper.isUIDarkTheme())
                Color.LTGRAY
            else
                Color.DKGRAY
        )
    }

    /**
     * Agrega el nuevo contenido a la notificación colapsada.
     *
     * @param userDataBytes - [UserDataBytes] con los valores a usar.
     * @param title - Título del contenido.
     * @param addSeparator - Indica si se debe agregar un separador antes del contenido.
     * */
    private fun addRemoteViewsContent(
        userDataBytes: UserDataBytes,
        title: String,
        addSeparator: Boolean
    ) {
        val percent = (100 * userDataBytes.bytes / userDataBytes.initialBytes)
            .toInt()

        val color = if (uiHelper.isUIDarkTheme())
            Color.LTGRAY
        else
            Color.DKGRAY

        if (addSeparator) {
            val separator = RemoteViews(packageName, R.layout.item_datwall_service_separator)
                .apply {

                    if (uiHelper.isUIDarkTheme())
                        setInt(
                            R.id.separator,
                            "setBackgroundColor",
                            Color.LTGRAY
                        )
                }

            remoteViews?.addView(R.id.content_view, separator)
        }

        val childRemotes = RemoteViews(packageName, R.layout.item_datwall_service).apply {
            setTextViewText(R.id.data_title, title)
            setInt(R.id.data_title, "setTextColor", color)

            setProgressBar(
                R.id.data_progress,
                100,
                percent,
                false
            )

            setTextViewText(
                R.id.data_percent,
                if (userDataBytes.isExpired()) "exp" else "$percent%"
            )
            setInt(R.id.data_percent, "setTextColor", color)
        }

        remoteViews?.addView(R.id.content_view, childRemotes)
    }

    /**
     * Agrega el nuevo contenido a la notificación expandida.
     *
     * @param userDataBytes - [UserDataBytes] con los valores a usar.
     * @param title - Título del contenido.
     * @param addSeparator - Indica si se debe agregar un separador antes del contenido.
     * */
    private fun addExpandedRemoteViewContent(
        userDataBytes: UserDataBytes,
        title: String,
        addSeparator: Boolean
    ) {
        val percent = (100 * userDataBytes.bytes / userDataBytes.initialBytes)
            .toInt()

        val color = if (uiHelper.isUIDarkTheme())
            Color.LTGRAY
        else
            Color.DKGRAY

        if (addSeparator) {
            val separator = RemoteViews(packageName, R.layout.item_datwall_service_separator)
                .apply {

                    if (uiHelper.isUIDarkTheme())
                        setInt(
                            R.id.separator,
                            "setBackgroundColor",
                            Color.LTGRAY
                        )
                }

            expandedRemoteViews?.addView(R.id.content_view, separator)
        }

        val childRemotes = RemoteViews(packageName, R.layout.item_datwall_service_expanded).apply {
            setTextViewText(R.id.data_title, title)
            setInt(R.id.data_title, "setTextColor", color)


            setProgressBar(
                R.id.data_progress,
                100,
                percent,
                false
            )

            setTextViewText(
                R.id.data_percent,
                if (userDataBytes.isExpired()) "exp" else "$percent%"
            )
            setInt(R.id.data_percent, "setTextColor", color)

            val dataBytes = DataUnitBytes(userDataBytes.bytes)

            setTextViewText(
                R.id.data_bytes,
                dataBytes.toString()
            )
            setInt(R.id.data_bytes, "setTextColor", color)
        }

        expandedRemoteViews?.addView(R.id.content_view, childRemotes)
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
        notificationBuilder.setSmallIcon(getIcon( rxBytes + txBytes))

        if (notificationEmpty) {
            setEmptyNotification()
        } else {
            notificationManager.notify(
                NotificationHelper.MAIN_NOTIFICATION_ID,
                notificationBuilder.apply {
                    setCustomContentView(this@DatwallService.remoteViews)
                    setCustomBigContentView(this@DatwallService.expandedRemoteViews)
                }.build()
            )
        }
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