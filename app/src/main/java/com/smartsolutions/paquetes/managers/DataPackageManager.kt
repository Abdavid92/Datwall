package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.managers.contracts.*
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
    private val simRepository: ISimRepository,
    private val miCubacelManager: IMiCubacelManager
): IDataPackageManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private var _buyMode: IDataPackageManager.ConnectionMode = IDataPackageManager.ConnectionMode.USSD
    override var buyMode: IDataPackageManager.ConnectionMode
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
                _buyMode = IDataPackageManager.ConnectionMode
                    .valueOf(it[PreferencesKeys.BUY_MODE] ?: IDataPackageManager.ConnectionMode.USSD.name)
            }
        }
    }

    override suspend fun configureDataPackages() {
        TODO("Hay que arreglarlo")
        ussdHelper.sendUSSDRequestLegacy("*133*1#")?.let { response ->
            //Texto del mensaje
            var text = response.string()

            //Linea predeterminada para llamadas
            val defaultSim = simManager.getDefaultVoiceSim()

            //Lte abilitada
            var enabledLte = false
            //3G abilitada
            var enabled3G = false

            //Si contiene los paquetes lte, habilito estos paquetes y borro
            //esa expresión de la variable text para que no se confunda a la hora
            //de verificar los paquetes 3G.
            if (text.contains("Paquetes LTE", true)) {
                enabledLte = true
                text = text.replace("Paquetes LTE", "", true)
            }

            if (text.contains("Paquetes", true)) {
                enabled3G = true
            }

            when {
                enabledLte && enabled3G -> defaultSim.network = Networks.NETWORK_3G_4G
                enabledLte && !enabled3G -> defaultSim.network = Networks.NETWORK_4G
                !enabledLte && enabled3G -> defaultSim.network = Networks.NETWORK_3G
                !enabledLte && !enabled3G -> defaultSim.network = Networks.NETWORK_NONE
            }

            //Fecha en la que se configuró esta linea.
            defaultSim.setupDate = System.currentTimeMillis()

            simRepository.update(defaultSim)
        }
    }

    override suspend fun setDataPackagesManualConfiguration(network: String) {
        val defaultSim = simManager.getDefaultVoiceSim().apply {
            this.network = network

            //Fecha en la que se configuró esta linea.
            this.setupDate = System.currentTimeMillis()
        }

        simRepository.update(defaultSim)
    }

    override suspend fun isConfiguredDataPackages(): Boolean {
        return try {
            simManager.getDefaultVoiceSim()
                .network != Networks.NETWORK_NONE
        } catch (e: IllegalStateException) {
            false
        }
    }

    @Throws(IllegalStateException::class, MissingPermissionException::class, UnprocessableRequestException::class)
    override suspend fun buyDataPackage(dataPackage: DataPackage, sim: Sim) {

        if (sim.packages.isEmpty()) {
            throw UnsupportedOperationException("The sim must be provide with relations")
        }

        if (!sim.packages.contains(dataPackage)) {
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
            simManager.getDefaultVoiceSim()
        else
            simManager.getSimByIndex(simIndex)

        if (smsBody.contains(DataPackages.PROMO_BONUS_KEY)) {
            val bytes = getBytesFromText("Bonos: ", smsBody)

            if (bytes != -1L) {
                userDataBytesManager.addPromoBonus(defaultSim.id, bytes)
            }
            return
        }

        DataPackages.PACKAGES.firstOrNull { smsBody.contains(it.smsKey) }?.let {

            dataPackageRepository.get(it.id)?.let { dataPackage ->
                userDataBytesManager.addDataBytes(dataPackage, defaultSim.id)
                purchasedPackagesManager.confirmPurchased(dataPackage.id, defaultSim.id)
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

    /*private suspend fun buyDataPackageForMiCubacel(dataPackage: DataPackage, sim: Sim) {
        if (sim.miCubacelAccount == null)
            throw NoSuchElementException(context.getString(R.string.account_not_found))


        val result = miCubacelManager.getProducts(sim.miCubacelAccount!!)

        var found = false

        for (group in result.getOrThrow()) {
            val product = group.firstOrNull { it.id == dataPackage.id }

            if (product != null) {
                found = true
                miCubacelManager.buyProduct(product.urlBuy, sim.miCubacelAccount!!)

                purchasedPackagesManager.newPurchased(
                    dataPackage.id,
                    sim.id,
                    IDataPackageManager.ConnectionMode.MiCubacel
                )
                break
            }
        }

        if (!found)
            throw NoSuchElementException(context.getString(R.string.product_not_found))
    }*/

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
            else -> throw IllegalStateException()
        }
    }
}