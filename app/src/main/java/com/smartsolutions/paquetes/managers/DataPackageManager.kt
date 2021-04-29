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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine


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

    private var _buyMode: IDataPackageManager.BuyMode = IDataPackageManager.BuyMode.USSD
    override var buyMode: IDataPackageManager.BuyMode
        get() = _buyMode
        set(value) {
            launch {
                context.dataStore.edit {
                    it[PreferencesKeys.BUY_MODE] = value.name
                }
            }
            _buyMode = value
        }

    init {
        launch {
            context.dataStore.data.collect {
                _buyMode = IDataPackageManager.BuyMode
                    .valueOf(it[PreferencesKeys.BUY_MODE] ?: IDataPackageManager.BuyMode.USSD.name)
            }
        }
    }

    override fun configureDataPackages() {
        launch {
            ussdHelper.sendUSSDRequestLegacy("*133*1#")?.let { response ->
                if (response.size > 1) {
                    val text = response[0].split("\n")

                    if (text.size == 4) {
                        print(text)
                    } else if (text.size == 5) {
                        print(text)
                    }
                }
            }
        }
    }

    override fun getPackages(): Flow<List<DataPackage>> {
        return dataPackageRepository.getAll()
    }

    override suspend fun buyDataPackage(id: String) {
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

    private suspend fun buyDataPackageForUSSD(id: String) {
        dataPackageRepository.get(id)?.let {

            if (it.ussd.isEmpty())
                throw IllegalStateException("Packages not configured")

            ussdHelper.sendUSSDRequestLegacy(it.ussd)
        }
    }

    private suspend fun buyDataPackageForMiCubacel(id: String) {
        val productGroups = miCubacelClientManager.getProducts()

        for (group in productGroups) {
            val product = group.firstOrNull { it.id == id }

            if (product != null) {
                miCubacelClientManager.buyProduct(product.urlBuy)
                break
            }
        }
    }
}