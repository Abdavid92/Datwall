package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.jvm.Throws


class DataPackageManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val dataPackageRepository: IDataPackageRepository,
    private val purchasedPackagesManager: PurchasedPackagesManager,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper,
    private val simManager: SimManager,
    private val simRepository: ISimRepository,
    private val miCubacelClientManager: MiCubacelClientManager
): IDataPackageManager {


    private var _buyMode: IDataPackageManager.BuyMode = IDataPackageManager.BuyMode.USSD
    override var buyMode: IDataPackageManager.BuyMode
        get() = _buyMode
        set(value) {
            GlobalScope.launch(Dispatchers.IO) {
                context.dataStore.edit {
                    it[PreferencesKeys.BUY_MODE] = value.name
                }
            }
            _buyMode = value
        }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            context.dataStore.data.collect {
                _buyMode = IDataPackageManager.BuyMode
                    .valueOf(it[PreferencesKeys.BUY_MODE] ?: IDataPackageManager.BuyMode.USSD.name)
            }
        }
    }

    override suspend fun configureDataPackages() {
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

    @Throws(IllegalStateException::class, MissingPermissionException::class, UnprocessableRequestException::class)
    override suspend fun buyDataPackage(dataPackage: DataPackage) {
        when (buyMode) {
            IDataPackageManager.BuyMode.USSD -> {
                buyDataPackageForUSSD(dataPackage)
            }
            IDataPackageManager.BuyMode.MiCubacel -> {
                buyDataPackageForMiCubacel(dataPackage)
            }
        }
    }

    override suspend fun registerDataPackage(smsBody: String, simIndex: Int) {

        val defaultSim = if (simIndex == -1)
            simManager.getDefaultVoiceSim()
        else
            simManager.getSimByIndex(simIndex)

        if (smsBody.contains(DataPackagesContract.PROMO_BONUS_KEY)) {
            userDataBytesManager.addPromoBonus(defaultSim.id)
            return
        }

        DataPackagesContract.PackagesList.firstOrNull { smsBody.contains(it.smsKey) }?.let {

            dataPackageRepository.get(it.id)?.let { dataPackage ->
                userDataBytesManager.addDataBytes(dataPackage, defaultSim.id)
                purchasedPackagesManager.confirmPurchased(dataPackage.id, defaultSim.id)
            }
        }
    }

    /**
     * Checkea si se pudo obtener el índice de la linea por la que entró el mensaje.
     * */
    private fun checkSimIndex(dataPackageId: String, simIndex: Int): Boolean {
        if (simIndex == -1) {

        }
        return true
    }

    private suspend fun buyDataPackageForUSSD(dataPackage: DataPackage) {
        val simDefault = simManager.getDefaultVoiceSim(true)

        if (!simDefault.packages.contains(dataPackage)) {
            throw IllegalStateException(context.getString(R.string.pkg_not_configured))
        }

        ussdHelper.sendUSSDRequestLegacy(buildUssd(simDefault, dataPackage),false)

        purchasedPackagesManager.newPurchased(
            dataPackage.id,
            simDefault.id,
            IDataPackageManager.BuyMode.USSD
        )
    }

    private suspend fun buyDataPackageForMiCubacel(dataPackage: DataPackage) {
        val productGroups = miCubacelClientManager.getProducts()

        for (group in productGroups) {
            val product = group.firstOrNull { it.id == dataPackage.id }

            if (product != null) {
                miCubacelClientManager.buyProduct(product.urlBuy)

                purchasedPackagesManager.newPurchased(
                    dataPackage.id,
                    simManager.getDefaultDataSim().id,
                    IDataPackageManager.BuyMode.MiCubacel
                )
                break
            }
        }
    }

    private fun buildUssd(sim: Sim, dataPackage: DataPackage): String {

        if (dataPackage.id == DataPackagesContract.DailyBag.id)
            return "*133*1*3#"

        var index = -1

        when (dataPackage.network) {
            Networks.NETWORK_4G -> {
                if (sim.network == Networks.NETWORK_3G_4G) {
                    index = 5
                } else if (sim.network == Networks.NETWORK_4G) {
                    index = 4
                }
            }
            Networks.NETWORK_3G_4G -> {
                if (sim.network == Networks.NETWORK_3G) {
                    index = 3
                } else if (sim.network == Networks.NETWORK_3G_4G) {
                    index = 4
                }
            }
            Networks.NETWORK_3G -> {
                index = 3
            }
        }

        return "*133*1*$index*${dataPackage.index}#"
    }
}