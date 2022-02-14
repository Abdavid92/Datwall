package com.smartsolutions.paquetes.managers

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.helpers.LegacyConfigurationHelper
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.serverApis.contracts.IActivationClient
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.MainActivity
import com.smartsolutions.paquetes.workers.ActivationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang.time.DateUtils
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val TAG_WORKER = "Activation_Worker"

class ActivationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val gson: Gson,
    private val client: IActivationClient,
    private val ussdHelper: USSDHelper,
    private val simManager: ISimManager,
    private val notificationHelper: NotificationHelper,
    private val legacyConfigurationHelper: LegacyConfigurationHelper
) : IActivationManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override val onConfirmPurchase: LiveData<Result<Unit>>
        get() = _onConfirmPurchase

    private val dataStore = context.internalDataStore

    private var deviceId: String? = null

    private var license: License? = null

    init {
        launch {
            dataStore.data.collect {
                it[PreferencesKeys.LICENSE]?.let { s ->

                    runCatching {
                        license = gson.fromJson(
                            decrypt(s),
                            License::class.java
                        )
                    }
                }
            }
        }
    }

    override suspend fun canWork(): Pair<Boolean, IActivationManager.ApplicationStatuses> {
        getLocalLicense()?.let {
            return processApplicationStatus(it)
        }

        return false to IActivationManager.ApplicationStatuses.Unknown
    }

    override suspend fun isInTrialPeriod(): Boolean {
        getLocalLicense()?.let {
            return it.inTrialPeriod()
        }

        return false
    }

    override fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener) {
        launch {
            val result = getLicense()

            try {
                val license = result.getOrThrow()

                val status = processApplicationStatus(license)

                withContext(Dispatchers.Main) {
                    when (status.second) {
                        IActivationManager.ApplicationStatuses.TooMuchOld ->
                            listener.onTooMuchOld(license)
                        IActivationManager.ApplicationStatuses.Purchased ->
                            listener.onPurchased(license)
                        IActivationManager.ApplicationStatuses.TrialPeriod ->
                            listener.onTrialPeriod(license, status.first)
                        IActivationManager.ApplicationStatuses.Discontinued ->
                            listener.onDiscontinued(license)
                        IActivationManager.ApplicationStatuses.Deprecated ->
                            listener.onDeprecated(license)
                        else -> listener.onFailed(Exception())
                    }
                }
            } catch (e: Exception) {

                withContext(Dispatchers.Main) {
                    listener.onFailed(e)
                }
            }
        }
    }

    override suspend fun transferCreditByUSSD(key: String, license: License): Result<Unit> {
        val price = license.androidApp.price
        if (key.isEmpty() || key.isBlank() || key.length != 4) {
            return Result.Failure(IllegalArgumentException())
        }

        dataStore.edit {
            it[PreferencesKeys.LICENSE] = encrypt(gson.toJson(license))
        }

        try {
            ussdHelper.sendUSSDRequestLegacy(
                "*234*1*${license.androidApp.phone}*$key*${price}#",
                false
            )
        } catch (e: Exception) {
            return Result.Failure(e)
        }
        return Result.Success(Unit)
    }

    override suspend fun confirmPurchase(
        smsBody: String,
        phone: String,
        simIndex: Int
    ): Result<Unit> {

        if (!phone.contains("PAGOxMOVIL", true) &&
            !phone.contains("Cubacel", true)
        ) {
            return Result.Failure(IllegalStateException())
        }

        getLocalLicense()?.let { license ->

            try {

                val androidApp = license.androidApp
                val price = androidApp.price.toString()

                val priceTransfermovil = "${androidApp.price}.00"

                if (smsBody.contains(androidApp.debitCard) && smsBody.contains(priceTransfermovil) && !smsBody.contains(
                        "telefono",
                        true
                    )
                ) {
                    license.transaction = readTransaction(smsBody)
                } else if (smsBody.contains(androidApp.phone) && smsBody.contains(price)) {
                    fillPhone(simIndex, license)
                } else {
                    return Result.Failure(NoSuchElementException())
                }

                license.isPurchased = true

                dataStore.edit {
                    it[PreferencesKeys.LICENSE] = encrypt(gson.toJson(license))
                }

                scheduleWorker()

                notifyLicencePurchased()

                return Result.Success(Unit).also {
                    _onConfirmPurchase.postValue(it)
                }
            } catch (e: Exception) {
                return Result.Failure(e)
            }
        }

        return Result.Failure(NullPointerException())
    }

    override suspend fun isWaitingPurchase(): Boolean {
        val time = dataStore.data.firstOrNull()?.get(PreferencesKeys.WAITING_PUCHASE) ?: 0L
        return System.currentTimeMillis() - time <= DateUtils.MILLIS_PER_DAY
    }

    override suspend fun setWaitingPurchase(value: Boolean) {
        if (value) {
            dataStore.edit {
                it[PreferencesKeys.WAITING_PUCHASE] = System.currentTimeMillis()
            }
        }else {
            dataStore.edit {
                it.remove(PreferencesKeys.WAITING_PUCHASE)
            }
        }
    }

    override suspend fun getLicense(): Result<License> {
        val result = client.getLicense(getDeviceId())

        if (result.isSuccess) {
            val licence = result.getOrThrow()

            if (licence.isPurchased && !licence.isRestored) {
                licence.isRestored = true
                scheduleWorker()

            } else if (!licence.isPurchased && legacyConfigurationHelper.isPurchased()) {
                licence.isPurchased = true
                scheduleWorker()
            }

            dataStore.edit {
                it[PreferencesKeys.LICENSE] = encrypt(gson.toJson(licence))
            }
        }

        return result
    }

    override suspend fun getLocalLicense(): License? {
        if (license == null) {

            dataStore.data.firstOrNull()
                ?.get(PreferencesKeys.LICENSE)
                ?.let {
                    runCatching {
                        val license = gson.fromJson(decrypt(it), License::class.java)

                        if (this.license == null)
                            this.license = license
                    }
                }
        }

        return license
    }

    private fun readTransaction(body: String): String {
        val toFind = "Nro. Transaccion"
        return try {
            body.substring(body.indexOf(toFind) + toFind.length, body.length)
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }

    private suspend fun fillPhone(simIndex: Int, license: License) {
        try {
            license.phone = simManager.getInstalledSims().firstOrNull { it.slotIndex == simIndex }?.phone
        } catch (e: Exception) {

        }
    }

    private fun scheduleWorker() {
        val workRequest = OneTimeWorkRequestBuilder<ActivationWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(TAG_WORKER)
            .build()

        val workManager = WorkManager.getInstance(context)

        workManager.cancelAllWorkByTag(TAG_WORKER)
        workManager.enqueue(workRequest)
    }

    private fun processApplicationStatus(license: License): Pair<Boolean, IActivationManager.ApplicationStatuses> {
        var canWork = false
        val statuses: IActivationManager.ApplicationStatuses

        when {
            licenseMuchOld(license) -> {
                statuses = IActivationManager.ApplicationStatuses.TooMuchOld
            }
            license.androidApp.status == ApplicationStatus.DISCONTINUED &&
                    !license.isPurchased -> {
                statuses = IActivationManager.ApplicationStatuses.Discontinued
            }
            license.androidApp.minVersion > BuildConfig.VERSION_CODE -> {
                statuses = IActivationManager.ApplicationStatuses.Deprecated
            }
            license.isPurchased -> {
                canWork = true
                statuses = IActivationManager.ApplicationStatuses.Purchased
            }
            else -> {
                canWork = license.inTrialPeriod()
                statuses = IActivationManager.ApplicationStatuses.TrialPeriod
            }
        }

        return canWork to statuses
    }

    /**
     * Indica si la licencia tiene más de un mes de antiguedad.
     * */
    private fun licenseMuchOld(license: License): Boolean {
        val calendar = Calendar.getInstance().apply {
            time = license.lastQuery
        }

        val currentCalendar = Calendar.getInstance()

        val isBigger = calendar <= currentCalendar

        return isBigger && calendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH)
    }

    private fun notifyLicencePurchased() {
        notificationHelper.notify(
            NotificationHelper.ALERT_NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.ALERT_CHANNEL_ID,
                R.drawable.ic_money_notification
            ).apply {
                setContentTitle(context.getString(R.string.purchased_done))
                setContentText(context.getString(R.string.purchased_description))
                setAutoCancel(true)
                setContentIntent(
                    PendingIntent.getActivity(
                        context,
                        12,
                        Intent(context, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        },
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        } else {
                            PendingIntent.FLAG_UPDATE_CURRENT
                        }
                    )
                )
            }.build()
        )
    }

    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    private suspend fun getDeviceId(): String {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {

                deviceId?.let {
                    return it
                }

                deviceId = dataStore.data
                    .firstOrNull()
                    ?.get(PreferencesKeys.DEVICE_ID)

                deviceId?.let {
                    return it
                }

                val wifiManager = ContextCompat
                    .getSystemService(context, WifiManager::class.java)
                    ?: throw NullPointerException()

                if (!wifiManager.isWifiEnabled) {
                    throw IllegalStateException("Wifi must be enabled.")
                }

                deviceId = getMacAddress() ?: Settings
                    .Secure
                    .getString(context.contentResolver, Settings.Secure.ANDROID_ID)

                dataStore.edit {
                    it[PreferencesKeys.DEVICE_ID] = deviceId!!
                }

                return deviceId!!
            }
            else -> {
                return Build.SERIAL
            }
        }
    }

    private fun getMacAddress(): String? {
        val macBuilder = StringBuilder()

        return try {
            NetworkInterface.getNetworkInterfaces()
                .toList()
                .firstOrNull { it.name.equals("p2p0", true) }?.let { nif ->
                    val macBytes = nif.hardwareAddress ?: return null

                    for (b in macBytes) {
                        macBuilder.append(String.format("%02X:", b))
                    }
                    if (macBuilder.isNotEmpty()) {
                        macBuilder.deleteCharAt(macBuilder.length - 1)
                    }
                }

            val mac = macBuilder.toString()

            if (mac.isBlank() || mac.isEmpty())
                return null

            mac
        } catch (ex: Exception) {
            null
        }
    }

    companion object {
        private val _onConfirmPurchase = MutableLiveData<Result<Unit>>()

        fun encrypt(data: String?): String {
            return String(Base64.encode(data?.toByteArray(), Base64.DEFAULT))
        }

        fun decrypt(data: String?): String {
            return String(Base64.decode(data, Base64.DEFAULT))
        }
    }
}