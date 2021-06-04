package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.lang.NumberFormatException
import javax.inject.Inject
import kotlin.jvm.Throws


class DataPackageManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val dataPackageRepository: IDataPackageRepository,
    private val purchasedPackagesManager: PurchasedPackagesManager,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper,
    private val simDelegate: SimDelegate,
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
            //Texto del mensaje dividido en saltos de linea
            val text = response.string().split("\n")

            //Índice basado en 1 de la linea activa
            val activeSimIndex = simDelegate.getActiveVoiceSimIndex()

            //Obtengo todos los paquetes de la base de datos
            dataPackageRepository.getAll().firstOrNull()?.let { packages ->
                //Por cada linea del texto
                text.forEach { menu ->

                    try {
                        /* Intento obtener el primer número del texto. Este número
                         * será el índice para ese conjunto de paquetes.*/
                        val index = Integer.parseInt(menu.trimStart()[0].toString())

                        when {
                            //Si es la bolsa diaria
                            menu.contains("Bolsa Diaria", true) -> {
                                //Busco la bolsa diaria de entre los paquetes
                                packages.firstOrNull {
                                    it.id == createDataPackageId(DataPackagesContract.DailyBag.name, DataPackagesContract.DailyBag.price)
                                }?.let { daily ->
                                    //Si estoy en la linea 1
                                    if (activeSimIndex == 1) {
                                        //Asigno el índice de la bolsa diaria a la linea 1
                                        daily.ussdSim1 = buildDataPackageUssdCode(index, daily.index)
                                        /*Activo la bolsa diaria para que pueda ser comprada
                                        * en la linea 1.*/
                                        daily.activeInSim1 = true
                                        //Si estoy en la linea 2
                                    } else if (activeSimIndex == 2) {
                                        //Asigno el índice de la bolsa diaria a la linea 2
                                        daily.ussdSim2 = buildDataPackageUssdCode(index, daily.index)
                                        /*Activo la bolsa diaria para que pueda ser comprada
                                        * en la linea 2.*/
                                        daily.activeInSim2 = true
                                    }
                                }
                            }
                            //Si son los paquetes de 3G
                            menu.contains("Paquetes", true) && !menu.contains("Paquetes LTE", true) -> {
                                //Activo los paquetes 3G para esta linea
                                activateDataPackages(
                                    index,
                                    activeSimIndex,
                                    DataPackage.NETWORK_3G_4G,
                                    packages)
                            }
                            //Si son los paquetes de LTE
                            menu.contains("Paquetes LTE", true) -> {
                                //Activo los paquetes LTE para esta linea
                                activateDataPackages(
                                    index,
                                    activeSimIndex,
                                    DataPackage.NETWORK_4G,
                                    packages)
                            }
                        }
                    } catch (e: NumberFormatException) {

                    }
                }
                //Por último actualizo los paquetes en base de datos.
                dataPackageRepository.update(packages)
            }
        }
    }

    override fun getPackages(): Flow<List<DataPackage>> = dataPackageRepository
        .getBySimId(simDelegate.getActiveVoiceSimIndex())

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

        if (smsBody.contains(DataPackagesContract.PROMO_BONUS_KEY)) {
            userDataBytesManager.addPromoBonus(simIndex)
            return
        }

        DataPackagesContract.PackagesList.firstOrNull { smsBody.contains(it.smsKey) }?.let {
            checkSimIndex(it.id, simIndex)

            dataPackageRepository.get(it.id)?.let { dataPackage ->
                userDataBytesManager.addDataBytes(dataPackage, simIndex)
                purchasedPackagesManager.confirmPurchased(dataPackage.id, simIndex)
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
        val sim = simDelegate.getActiveVoiceSimIndex()

        if ((sim == 1 && !dataPackage.activeInSim1) || (sim == 2 && !dataPackage.activeInSim2)) {
            throw IllegalStateException(context.getString(R.string.pkg_not_configured))
        }

        ussdHelper.sendUSSDRequestLegacy(if (sim == 1)
            dataPackage.ussdSim1!!
        else
            dataPackage.ussdSim2!!,
            false)

        purchasedPackagesManager.newPurchased(
            dataPackage.id,
            sim,
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
                    simDelegate.getActiveDataSimIndex(),
                    IDataPackageManager.BuyMode.MiCubacel
                )
                break
            }
        }
    }

    private fun activateDataPackages(
        index: Int,
        activeSimIndex: Int,
        @DataPackage.Networks
        network: String,
        packages: List<DataPackage>) {

        packages.forEach {
            if (it.network == network && it.id !=
                createDataPackageId(DataPackagesContract.DailyBag.name, DataPackagesContract.DailyBag.price)) {
                if (activeSimIndex == 1) {
                    it.ussdSim1 = buildDataPackageUssdCode(index, it.index)
                    it.activeInSim1 = true
                }
                else if (activeSimIndex == 2) {
                    it.ussdSim2 = buildDataPackageUssdCode(index, it.index)
                    it.activeInSim2 = true
                }
            }
        }
    }
}