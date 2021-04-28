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
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.micubacel.models.Product
import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
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
    private val dataPackageRepository: IDataPackageRepository,
    private val purchasedPackageRepository: IPurchasedPackageRepository,
    private val ussdHelper: USSDHelper,
    private val miCubacelClientManager: MiCubacelClientManager
): IDataPackageManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override var buyMode: IDataPackageManager.BuyMode = IDataPackageManager.BuyMode.USSD
        set(value) {
            launch {
                context.dataStore.edit {
                    it[PreferencesKeys.BUY_MODE] = value.name
                }
            }
            field = value
        }

    init {

        launch {
            context.dataStore.data.collect {
                buyMode = IDataPackageManager.BuyMode
                    .valueOf(it[PreferencesKeys.BUY_MODE] ?: IDataPackageManager.BuyMode.USSD.name)
            }
        }
    }

    override fun configureDataPackages() {
        ussdHelper.sendUSSDRequestLegacy("*133*1#", object : USSDHelper.Callback {
            override fun onSuccess(response: Array<CharSequence>) {

            }

            override fun onFail(errorCode: Int, message: String) {

            }
        })
    }

    override fun getPackages(): Flow<List<DataPackage>> {
        return dataPackageRepository.getAll()
    }

    override fun buyDataPackage(id: String) {
        when (buyMode) {
            IDataPackageManager.BuyMode.USSD -> {
                buyDataPackageForUSSD(id)
            }
            IDataPackageManager.BuyMode.MiCubacel -> {
                buyDataPackageForMiCubacel(id)
            }
        }
    }

    override fun registerDataPackage(smsBody: String) {

    }

    /*fun setBuyMode(mode: IDataPackageManager.BuyMode) {
        launch {
            context.dataStore.edit {
                it[PreferencesKeys.BUY_MODE] = mode.name
            }
        }
    }*/

    override fun getHistory(): Flow<List<PurchasedPackage>> =
        purchasedPackageRepository.getAll()

    override fun clearHistory() {
        launch {
            purchasedPackageRepository.getAll().collect {
                purchasedPackageRepository.delete(it)
            }
        }
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

    private fun buyDataPackageForUSSD(id: String) {
        launch {
            dataPackageRepository.get(id)?.let {
                ussdHelper.sendUSSDRequestLegacy(it.ussd)
            }
        }
    }

    private fun buyDataPackageForMiCubacel(id: String) {
        launch {
            miCubacelClientManager.getProducts(object : MiCubacelClientManager.Callback<List<ProductGroup>> {
                override fun onSuccess(response: List<ProductGroup>) {

                }

                override fun onFail(throwable: Throwable) {
                    TODO("Not yet implemented")
                }

            })
        }
    }
}