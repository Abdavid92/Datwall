package com.smartsolutions.paquetes.managers

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Base64
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.work.*
import com.google.gson.Gson
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.workers.ActivationWorker
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.LegacyConfigurationHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.NetworkInterface
import java.util.*
import javax.inject.Inject
import kotlin.NoSuchElementException

class ActivationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val registrationClient: IRegistrationClient,
    private val ussdHelper: USSDHelper,
    private val gson: Gson,
    private val simManager: ISimManager,
    private val legacyConfiguration: LegacyConfigurationHelper
) : IActivationManager {

    override suspend fun canWork(): Pair<Boolean, IActivationManager.ApplicationStatuses> {
        getSavedDeviceApp()?.let {
            return processApplicationStatus(it)
        }
        return Pair(false, IActivationManager.ApplicationStatuses.Unknown)
    }

    override suspend fun isInTrialPeriod(): Boolean {
        getSavedDeviceApp()?.let {
            return it.inTrialPeriod()
        }
        return false
    }

    override suspend fun getDevice(): Result<Device> {

        val deviceId = getDeviceId()
        val packageName = context.packageName

        val device = Device(
            deviceId,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.VERSION.SDK_INT
        )

        val deviceApp = DeviceApp(
            id = DeviceApp.buildDeviceAppId(packageName, deviceId),
            purchased = legacyConfiguration.isPurchased(),
            restored = legacyConfiguration.isPurchased(),
            trialPeriod = true,
            lastQuery = Date(System.currentTimeMillis()),
            transaction = null,
            phone = null,
            waitingPurchase = false,
            deviceId = deviceId,
            androidAppPackageName = packageName,
            createdAt = Date(System.currentTimeMillis())
        )

        device.deviceApps = listOf(deviceApp)

        val result = registrationClient.getOrRegister(device)

        if (result.isSuccess) {
            result.getOrNull()
                ?.deviceApps
                ?.firstOrNull { it.androidAppPackageName == packageName }
                ?.let { onlineDeviceApp ->
                    context.dataStore.edit {
                        it[PreferencesKeys.DEVICE_APP] = encrypt(gson.toJson(onlineDeviceApp))
                    }
                }
        }

        return result
    }

    override suspend fun getDeviceApp(ignoreCache: Boolean): Result<DeviceApp> {

        if (!ignoreCache) {
            val savedDeviceApp = getSavedDeviceApp()

            if (savedDeviceApp != null && isNotOldDeviceApp(savedDeviceApp)) {
                return Result.Success(savedDeviceApp)
            }
        }

        val deviceId = getDeviceId()
        val packageName = context.packageName

        val deviceApp = DeviceApp(
            id = DeviceApp.buildDeviceAppId(packageName, deviceId),
            purchased = legacyConfiguration.isPurchased(),
            restored = legacyConfiguration.isPurchased(),
            trialPeriod = true,
            lastQuery = Date(System.currentTimeMillis()),
            transaction = null,
            phone = null,
            waitingPurchase = false,
            deviceId = deviceId,
            androidAppPackageName = packageName,
            createdAt = Date(System.currentTimeMillis())
        )
        val result = registrationClient.getOrRegisterDeviceApp(deviceApp.id, deviceApp)

        if (result.isSuccess) {
            result.getOrNull()?.let { onlineDeviceApp ->
                context.dataStore.edit {
                    it[PreferencesKeys.DEVICE_APP] = encrypt(gson.toJson(onlineDeviceApp))
                }
            }
        }

        return result
    }

    override suspend fun getSavedDeviceApp(): DeviceApp? {
        context.dataStore.data
            .firstOrNull()
            ?.get(PreferencesKeys.DEVICE_APP)
            ?.let {
                try {
                    return gson.fromJson(decrypt(it), DeviceApp::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        return null
    }

    override fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener) {
        GlobalScope.launch(Dispatchers.IO) {
            val result = getDevice()

            withContext(Dispatchers.Main) {
                try {
                    val deviceApp = result.getOrThrow()
                        .deviceApps!!.first { it.androidAppPackageName == context.packageName }

                    val status = processApplicationStatus(deviceApp)

                    when (status.second) {
                        IActivationManager.ApplicationStatuses.TooMuchOld ->
                            listener.onTooMuchOld(deviceApp)
                        IActivationManager.ApplicationStatuses.Purchased ->
                            listener.onPurchased(deviceApp)
                        IActivationManager.ApplicationStatuses.TrialPeriod ->
                            listener.onTrialPeriod(deviceApp, status.first)
                        IActivationManager.ApplicationStatuses.Discontinued ->
                            listener.onDiscontinued(deviceApp)
                        IActivationManager.ApplicationStatuses.Deprecated ->
                            listener.onDeprecated(deviceApp)
                        else -> listener.onFailed(Exception())
                    }
                } catch (e: Exception) {
                    listener.onFailed(e)
                }
            }
        }
    }

    override suspend fun beginActivation(deviceApp: DeviceApp): Result<Unit> {
        /*return try {
            registrationClient.updateDeviceApp(getDeviceApp().getOrThrow().apply {
                waitingPurchase = true
            })
        } catch (e: Exception) {
            Result.Failure(e)
        }*/

        //Temp: NO es la implementación definitiva.
        context.dataStore.edit {
            it[PreferencesKeys.WAITING_PURCHASED] = true
            it[PreferencesKeys.DEVICE_APP] = encrypt(gson.toJson(deviceApp))
        }
        return Result.Success(Unit)
    }

    override suspend fun transferCreditByUSSD(key: String, deviceApp: DeviceApp): Result<Unit> {
        val price = deviceApp.androidApp.price
        if (key.isEmpty() || key.isBlank() || key.length != 4 || price - price != 0){
            return Result.Failure(IllegalArgumentException())
        }

        if (context.dataStore.data.first()[PreferencesKeys.WAITING_PURCHASED] == false)
            throw IllegalStateException("First call the method beginActivation()")

        try {
            ussdHelper.sendUSSDRequestLegacy(
                "*234*1*${deviceApp.androidApp.phone}*$key*${price.toInt()}#",
                false)
        }catch (e: Exception){
            return Result.Failure(e)
        }
        return Result.Success(Unit)
    }

    override suspend fun confirmPurchase(smsBody: String, phone: String, simIndex: Int): Result<Unit> {
        if (!isWaitingPurchased() || !phone.contains("PAGOxMOVIL", true) &&
            !phone.contains("Cubacel", true)){
            return Result.Failure(IllegalStateException())
        }

       try {
           val data = context.dataStore.data.first()[PreferencesKeys.DEVICE_APP]
           val deviceApp = gson.fromJson(
                decrypt(data),
                DeviceApp::class.java
           )

           val androidApp = deviceApp.androidApp
           val price = androidApp.price.toString()

           val priceTransfermovil = "${androidApp.price}.00"

           if (smsBody.contains(androidApp.debitCard) && smsBody.contains(priceTransfermovil)){
               deviceApp.transaction = readTransaction(smsBody)
           }else if(smsBody.contains(androidApp.phone) && smsBody.contains(price)) {
               fillPhone(simIndex, deviceApp)
           }else{
               return Result.Failure(NoSuchElementException())
           }

           deviceApp.purchased = true
           deviceApp.waitingPurchase = false

           context.dataStore.edit {
               it[PreferencesKeys.DEVICE_APP] = encrypt(gson.toJson(deviceApp))
               it[PreferencesKeys.WAITING_PURCHASED] = false
               scheduleWorker()
           }

           return Result.Success(Unit)
        }catch (e: Exception){
            return Result.Failure(e)
        }

    }

    override suspend fun isWaitingPurchased(): Boolean {
        return context.dataStore.data.firstOrNull()?.get(PreferencesKeys.WAITING_PURCHASED) == true
    }

    /**
     * Procesa el estado de el deviceApp.
     *
     * @return [Pair] con una Boolean indicando si se puede trabajar y la razón.
     * */
    private fun processApplicationStatus(deviceApp: DeviceApp): Pair<Boolean, IActivationManager.ApplicationStatuses> {
        var canWork = false
        val statuses: IActivationManager.ApplicationStatuses
        
        when {
            deviceAppMuchOld(deviceApp) -> {
                statuses = IActivationManager.ApplicationStatuses.TooMuchOld
            }
            deviceApp.androidApp.status == ApplicationStatus.DISCONTINUED &&
                    !deviceApp.purchased -> {
                statuses = IActivationManager.ApplicationStatuses.Discontinued
            }
            deviceApp.androidApp.minVersion > BuildConfig.VERSION_CODE -> {
                statuses = IActivationManager.ApplicationStatuses.Deprecated
            }
            deviceApp.purchased -> {
                canWork = true
                statuses = IActivationManager.ApplicationStatuses.Purchased
            }
            else -> {
                canWork = deviceApp.inTrialPeriod()
                statuses = IActivationManager.ApplicationStatuses.TrialPeriod
            }
        }
        return Pair(canWork, statuses)
    }

    /**
     * Indica si el deviceApp tiene más de un mes de antiguedad.
     * */
    private fun deviceAppMuchOld(deviceApp: DeviceApp): Boolean {
        val calendar = Calendar.getInstance().apply {
            time = deviceApp.lastQuery
        }

        val currentCalendar = Calendar.getInstance()

        val isBigger = calendar <= currentCalendar

        return isBigger && calendar.get(Calendar.MONTH) != currentCalendar.get(Calendar.MONTH)
    }

    private fun readTransaction(body: String): String {
        val toFind = "Nro. Transaccion "
        return body.substring(body.indexOf(toFind) + toFind.length, body.length)
    }

    private suspend fun fillPhone(simIndex: Int, deviceApp: DeviceApp) {
        try {
            deviceApp.phone = simManager.getSimByIndex(simIndex).phone
        } catch (e: Exception) {

        }
    }

    private fun scheduleWorker() {
        val workRequest = OneTimeWorkRequestBuilder<ActivationWorker>()
            .setConstraints(Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build())
            .build()

        WorkManager.getInstance(context)
            .enqueue(workRequest)
    }

    @SuppressLint("HardwareIds")
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

    private fun isNotOldDeviceApp(savedDeviceApp: DeviceApp): Boolean {
        val time = savedDeviceApp.lastQuery.time + (60000 * 5)
        val current = System.currentTimeMillis()

        return time > current
    }

    private fun encrypt(data: String?): String {
        return String(Base64.encode(data?.toByteArray(), Base64.DEFAULT))
    }

    private fun decrypt(data: String?): String {
        return String(Base64.decode(data, Base64.DEFAULT))
    }
}