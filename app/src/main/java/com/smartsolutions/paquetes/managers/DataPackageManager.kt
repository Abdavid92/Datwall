package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.*
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws


class DataPackageManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val dataPackageRepository: IDataPackageRepository,
    private val purchasedPackagesManager: IPurchasedPackagesManager,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper,
    private val simManager: ISimManager,
    private val simRepository: ISimRepository
): IDataPackageManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val dataStore = context.internalDataStore

    private var _buyMode: IDataPackageManager.ConnectionMode = IDataPackageManager.ConnectionMode.USSD
    override var buyMode: IDataPackageManager.ConnectionMode
        get() = _buyMode
        set(value) {
            launch(Dispatchers.IO) {
                dataStore.edit {
                    it[PreferencesKeys.BUY_MODE] = value.name
                }
            }
            _buyMode = value
        }

    init {
        launch(Dispatchers.IO) {
            dataStore.data.collect {
                _buyMode = IDataPackageManager.ConnectionMode
                    .valueOf(it[PreferencesKeys.BUY_MODE] ?: IDataPackageManager.ConnectionMode.USSD.name)
            }
        }
    }

    override suspend fun createOrUpdateDataPackages() {
        withContext(Dispatchers.IO) {
            val oldVersion = dataStore.data
                .firstOrNull()?.get(PreferencesKeys.CURRENT_PACKAGES_VERSION) ?: 0

            if (oldVersion < DataPackages.PACKAGES_VERSION) {
                dataPackageRepository.createOrUpdate(DataPackages.PACKAGES.toList())

                dataStore.edit {
                    it[PreferencesKeys.CURRENT_PACKAGES_VERSION] = DataPackages.PACKAGES_VERSION
                }
            }
        }
    }

    override suspend fun configureDataPackages() {
        val plainsResultText = ussdHelper.sendUSSDRequestLegacy("*133#")
        val lteResultText = ussdHelper.sendUSSDRequestLegacy("*133*1#")

        //Indica si los planes están abilitados
        var enabledCombinedPlains = false

        //Indica si los paquetes 4G están abilitados
        var enabledLte = false

        //Linea predeterminada para llamadas
        simManager.getDefaultSimBoth(SimDelegate.SimType.VOICE)?.let { defaultSim ->

            plainsResultText?.let {
                val text = it.string()

                if (text.contains("Planes", true))
                    enabledCombinedPlains = true
            }

            lteResultText?.let {
                val text = it.string()

                if (text.contains("Paquetes LTE", true))
                    enabledLte = true
            }

            when {
                enabledCombinedPlains && enabledLte -> defaultSim.network = Networks.NETWORK_3G_4G
                enabledCombinedPlains && !enabledLte -> defaultSim.network = Networks.NETWORK_3G
                !enabledCombinedPlains && enabledLte -> defaultSim.network = Networks.NETWORK_4G
                !enabledCombinedPlains && !enabledLte -> defaultSim.network = Networks.NETWORK_NONE
            }

            //Fecha en la que se configuró esta linea.
            defaultSim.setupDate = System.currentTimeMillis()

            withContext(Dispatchers.IO) {
                simRepository.update(defaultSim)
            }
        }
    }

    override suspend fun setDataPackagesManualConfiguration(network: String, sim: Sim) {
        sim.apply {
            this.network = network

            //Fecha en la que se configuró esta linea.
            this.setupDate = System.currentTimeMillis()
        }

        withContext(Dispatchers.IO) {
           simRepository.update(sim)
        }
    }

    override suspend fun isConfiguredDataPackages(): Boolean {
        return try {
            simManager.getDefaultSimBoth(SimDelegate.SimType.VOICE)
                ?.network ?: Networks.NETWORK_NONE != Networks.NETWORK_NONE
        } catch (e: IllegalStateException) {
            false
        }
    }

    @Throws(IllegalStateException::class, MissingPermissionException::class, UnprocessableRequestException::class)
    override suspend fun buyDataPackage(dataPackage: DataPackage, sim: Sim) {

        if (sim.packages.isEmpty()) {
            throw UnsupportedOperationException("The sim must be provide with relations")
        }

        if (!sim.packages.contains(dataPackage) && dataPackage.id != DataPackages.PackageId.MessagingBag) {
            throw IllegalStateException(context.getString(R.string.pkg_not_configured))
        }

        when (buyMode) {
            IDataPackageManager.ConnectionMode.USSD -> {
                buyDataPackageForUSSD(dataPackage, sim)
            }
            IDataPackageManager.ConnectionMode.MiCubacel -> {
                //buyDataPackageForMiCubacel(dataPackage, sim)
            }
            IDataPackageManager.ConnectionMode.Unknown ->
                throw UnsupportedOperationException("Unknown buy mode")
        }
    }

    override suspend fun registerDataPackage(smsBody: String, simIndex: Int) {

        val defaultSim = if (simIndex == -1)
            simManager.getDefaultSimBoth(SimDelegate.SimType.VOICE)
        else
            simManager.getInstalledSims().firstOrNull { it.slotIndex == simIndex } ?:
            simManager.getDefaultSimBoth(SimDelegate.SimType.VOICE)

        if (smsBody.contains(DataPackages.PROMO_BONUS_KEY)) {
            val bytes = getBytesFromText("Bonos: ", smsBody)

            if (bytes != -1L) {
                defaultSim?.id?.let {
                    userDataBytesManager.addPromoBonus(it, bytes)
                }
            }
            return
        } else if (smsBody.contains(DataPackages.MESSAGING_BAG_KEY)){
            defaultSim?.id?.let { simId ->
                DataPackages.PACKAGES.firstOrNull { it.id == DataPackages.PackageId.MessagingBag }?.let {
                    userDataBytesManager.addMessagingBag(simId, it)
                }
            }
            return
        }

        DataPackages.PACKAGES.firstOrNull { smsBody.contains(it.smsKey) }?.let {

            withContext(Dispatchers.IO){
                dataPackageRepository.get(it.id)
            }?.let { dataPackage ->
                defaultSim?.id?.let {
                    userDataBytesManager.addDataBytes(dataPackage, it)
                    purchasedPackagesManager.confirmPurchased(dataPackage.id, it)
                }
            }
        }
    }

    private suspend fun buyDataPackageForUSSD(dataPackage: DataPackage, sim: Sim) {

        ussdHelper.sendUSSDRequestLegacy(buildUssd(dataPackage),false)

        purchasedPackagesManager.newPurchased(
            dataPackage.id,
            sim.id,
            IDataPackageManager.ConnectionMode.USSD
        )
    }

    private fun buildUssd(dataPackage: DataPackage): String {

        if (dataPackage.id == DataPackages.PackageId.DailyBag)
            return "*133*1*3#"

        return when (dataPackage.network) {
            Networks.NETWORK_4G -> {
                "*133*1*4*${dataPackage.index}#"
            }
            Networks.NETWORK_3G,
            Networks.NETWORK_3G_4G -> {
                "*133*5*${dataPackage.index}#"
            }
            Networks.NETWORK_NONE -> {
                "*133*1*${dataPackage.index}#"
            }
            else -> throw IllegalStateException()
        }
    }
}