package com.smartsolutions.paquetes.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


class DataPackageManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val dataPackageRepository: IDataPackageRepository
): IDataPackageManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private lateinit var buyMode: IDataPackageManager.BuyMode

    init {

        launch {
            context.dataStore.data.collect {
                buyMode = IDataPackageManager.BuyMode.valueOf(it[PreferencesKeys.BUY_MODE] ?: "MiCubacel")
            }
        }
    }

    override fun getPackages(): Flow<List<DataPackage>> {
        return dataPackageRepository.getAll()
    }

    override fun buyDataPackage(id: Int) {
        TODO("Not yet implemented")
    }

    override fun registerDataPackage(smsBody: String) {

    }

    override fun setBuyMode(mode: IDataPackageManager.BuyMode) {
        TODO("Not yet implemented")
    }

    override fun getHistory(): Flow<List<PurchasedPackage>> {
        TODO("Not yet implemented")
    }

    override fun clearHistory() {
        TODO("Not yet implemented")
    }

    fun getActiveSimIndex(): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED
            ) {
                return -1
            }

            val subscriptionManager = ContextCompat
                .getSystemService(context, SubscriptionManager::class.java) ?: throw NullPointerException()

            val info = subscriptionManager.getActiveSubscriptionInfo(SubscriptionManager
                .getDefaultDataSubscriptionId())

            return info.simSlotIndex
        } else {
            try {

                val telephonyManager = ContextCompat
                    .getSystemService(context, TelephonyManager::class.java) ?: throw NullPointerException()

                val method = telephonyManager.javaClass.getDeclaredMethod("getDefaultSim")

                method.isAccessible = true

                return method.invoke(telephonyManager) as Int

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return -1
    }
}