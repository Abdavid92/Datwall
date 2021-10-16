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
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
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
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ActivationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val gson: Gson,
    private val client: IActivationClient,
    private val ussdHelper: USSDHelper,
    private val simManager: ISimManager
) : IActivationManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val dataStore = context.internalDataStore

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
        if (key.isEmpty() || key.isBlank() || key.length != 4){
            return Result.Failure(IllegalArgumentException())
        }

        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[PreferencesKeys.WAITING_PURCHASED] = true
                it[PreferencesKeys.LICENCE] = encrypt(gson.toJson(license))
            }
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
            val data = withContext(Dispatchers.IO){
                dataStore.data.first()[PreferencesKeys.LICENCE]
            }
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

            withContext(Dispatchers.IO) {
                dataStore.edit {
                    it[PreferencesKeys.LICENCE] = encrypt(gson.toJson(license))
                    it[PreferencesKeys.WAITING_PURCHASED] = false
                    withContext(Dispatchers.Default) {
                        scheduleWorker()
                    }
                }
            }

            return Result.Success(Unit)
        } catch (e: Exception) {
            return Result.Failure(e)
        }
    }

    override suspend fun isWaitingPurchased(): Boolean {
        return withContext(Dispatchers.IO){
            dataStore.data.firstOrNull()?.get(PreferencesKeys.WAITING_PURCHASED) == true
        }
    }

    override suspend fun getLicense(): Result<License> {
        val result = client.getLicense(getDeviceId())

        if (result.isSuccess) {
            withContext(Dispatchers.IO) {
                dataStore.edit {
                    it[PreferencesKeys.LICENCE] = encrypt(gson.toJson(result.getOrThrow()))
                }
            }
        }

        return result
    }

    override suspend fun getLocalLicense(): License? {
       withContext(Dispatchers.IO){
           dataStore.data.firstOrNull()
               ?.get(PreferencesKeys.LICENCE)
       }?.let {
                try {
                    return gson.fromJson(decrypt(it), License::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
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
            license.phone = withContext(Dispatchers.IO){
                simManager.getSimBySlotIndex(simIndex)?.phone
            }
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

    @SuppressLint("HardwareIds")
    @Suppress("DEPRECATION")
    private suspend fun getDeviceId(): String {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> {

                var deviceId = withContext(Dispatchers.IO){
                    dataStore.data
                        .firstOrNull()
                        ?.get(PreferencesKeys.DEVICE_ID)
                }

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

                withContext(Dispatchers.IO) {
                    context.settingsDataStore.edit {
                        it[PreferencesKeys.DEVICE_ID] = deviceId!!
                    }
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
        fun encrypt(data: String?): String {
            return String(Base64.encode(data?.toByteArray(), Base64.DEFAULT))
        }

        fun decrypt(data: String?): String {
            return String(Base64.decode(data, Base64.DEFAULT))
        }
    }
}