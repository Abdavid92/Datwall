package com.smartsolutions.paquetes.managers

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.gson.Gson
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager2
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.serverApis.contracts.IActivationClient
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.workers.ActivationWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ActivationManager2 @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val gson: Gson,
    private val client: IActivationClient,
    private val ussdHelper: USSDHelper,
    private val simManager: ISimManager
) : IActivationManager2, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override suspend fun canWork(): Pair<Boolean, IActivationManager2.ApplicationStatuses> {
        getLocalLicense()?.let {
            return processApplicationStatus(it)
        }

        return false to IActivationManager2.ApplicationStatuses.Unknown
    }

    override suspend fun isInTrialPeriod(): Boolean {
        getLocalLicense()?.let {
            return it.inTrialPeriod()
        }

        return false
    }

    override fun getApplicationStatus(listener: IActivationManager2.ApplicationStatusListener) {
        launch {
            val result = getLicense()

            try {
                val license = result.getOrThrow()

                val status = processApplicationStatus(license)

                when (status.second) {
                    IActivationManager2.ApplicationStatuses.TooMuchOld ->
                        listener.onTooMuchOld(license)
                    IActivationManager2.ApplicationStatuses.Purchased ->
                        listener.onPurchased(license)
                    IActivationManager2.ApplicationStatuses.TrialPeriod ->
                        listener.onTrialPeriod(license, status.first)
                    IActivationManager2.ApplicationStatuses.Discontinued ->
                        listener.onDiscontinued(license)
                    IActivationManager2.ApplicationStatuses.Deprecated ->
                        listener.onDeprecated(license)
                    else -> listener.onFailed(Exception())
                }
            } catch (e: Exception) {
                listener.onFailed(e)
            }
        }
    }

    override suspend fun transferCreditByUSSD(key: String, license: License): Result<Unit> {
        val price = license.androidApp.price
        if (key.isEmpty() || key.isBlank() || key.length != 4 || price - price != 0){
            return Result.Failure(IllegalArgumentException())
        }

        context.dataStore.edit {
            it[PreferencesKeys.WAITING_PURCHASED] = true
            it[PreferencesKeys.LICENCE] = encrypt(gson.toJson(license))
        }

        try {
            ussdHelper.sendUSSDRequestLegacy(
                "*234*1*${license.androidApp.phone}*$key*${price}#",
                false)
        }catch (e: Exception){
            return Result.Failure(e)
        }
        return Result.Success(Unit)
    }

    override suspend fun confirmPurchase(
        smsBody: String,
        phone: String,
        simIndex: Int
    ): Result<Unit> {
        if (!isWaitingPurchased() || !phone.contains("PAGOxMOVIL", true) &&
            !phone.contains("Cubacel", true)){
            return Result.Failure(IllegalStateException())
        }

        try {
            val data = context.dataStore.data.first()[PreferencesKeys.LICENCE]
            val license = gson.fromJson(
                decrypt(data),
                License::class.java
            )

            val androidApp = license.androidApp
            val price = androidApp.price.toString()

            val priceTransfermovil = "${androidApp.price}.00"

            if (smsBody.contains(androidApp.debitCard) && smsBody.contains(priceTransfermovil)){
                license.transaction = readTransaction(smsBody)
            }else if(smsBody.contains(androidApp.phone) && smsBody.contains(price)) {
                fillPhone(simIndex, license)
            }else{
                return Result.Failure(NoSuchElementException())
            }

            license.isPurchased = true

            context.dataStore.edit {
                it[PreferencesKeys.LICENCE] = encrypt(gson.toJson(license))
                it[PreferencesKeys.WAITING_PURCHASED] = false
                scheduleWorker()
            }

            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Failure(e)
        }
    }

    override suspend fun isWaitingPurchased(): Boolean {
        return context.dataStore.data.firstOrNull()?.get(PreferencesKeys.WAITING_PURCHASED) == true
    }

    override suspend fun getLicense(): Result<License> {
        return client.getLicense(getDeviceId())
    }

    override suspend fun getLocalLicense(): License? {
        context.dataStore.data.firstOrNull()
            ?.get(PreferencesKeys.LICENCE)
            ?.let {
                try {
                    return gson.fromJson(decrypt(it), License::class.java)
                } catch (e: Exception) {

                }
            }

        return null
    }

    private fun readTransaction(body: String): String {
        val toFind = "Nro. Transaccion "
        return body.substring(body.indexOf(toFind) + toFind.length, body.length)
    }

    private suspend fun fillPhone(simIndex: Int, license: License) {
        try {
            license.phone = simManager.getSimBySlotIndex(simIndex)?.phone
        } catch (e: Exception) {

        }
    }

    private fun scheduleWorker() {
        val workRequest = OneTimeWorkRequestBuilder<ActivationWorker>()
            .setConstraints(
                Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }

    private fun processApplicationStatus(license: License): Pair<Boolean, IActivationManager2.ApplicationStatuses> {
        var canWork = false
        val statuses: IActivationManager2.ApplicationStatuses

        when {
            licenseMuchOld(license) -> {
                statuses = IActivationManager2.ApplicationStatuses.TooMuchOld
            }
            license.androidApp.status == ApplicationStatus.DISCONTINUED &&
                    !license.isPurchased -> {
                statuses = IActivationManager2.ApplicationStatuses.Discontinued
            }
            license.androidApp.minVersion > BuildConfig.VERSION_CODE -> {
                statuses = IActivationManager2.ApplicationStatuses.Deprecated
            }
            license.isPurchased -> {
                canWork = true
                statuses = IActivationManager2.ApplicationStatuses.Purchased
            }
            else -> {
                canWork = license.inTrialPeriod()
                statuses = IActivationManager2.ApplicationStatuses.TrialPeriod
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

    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    private suspend fun getDeviceId(): String {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {

                var deviceId = context.dataStore.data
                    .firstOrNull()
                    ?.get(PreferencesKeys.DEVICE_ID)

                deviceId?.let {
                    return it
                }

                val wifiManager = ContextCompat
                    .getSystemService(context, WifiManager::class.java) ?:
                throw NullPointerException()

                if (!wifiManager.isWifiEnabled) {
                    throw IllegalStateException("Wifi must be enabled.")
                }

                deviceId = getMacAddress() ?: Settings
                    .Secure
                    .getString(context.contentResolver, Settings.Secure.ANDROID_ID)

                context.dataStore.edit {
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

    private fun encrypt(data: String?): String {
        return String(Base64.encode(data?.toByteArray(), Base64.DEFAULT))
    }

    private fun decrypt(data: String?): String {
        return String(Base64.decode(data, Base64.DEFAULT))
    }
}