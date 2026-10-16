package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.PackageNotConfiguredException
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.*
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.*
import com.smartsolutions.paquetes.managers.sims.SimType
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

                //Desde la versión 4 la configuración de la línea es "puede comprar planes sí/no":
                //las líneas que admitían algún tipo de paquete pasan a admitir los planes.
                if (oldVersion < 4) {
                    val sims = simRepository.all()
                        .filter { it.network != Networks.NETWORK_NONE && it.network != Networks.NETWORK_3G_4G }
                        .onEach { it.network = Networks.NETWORK_3G_4G }

                    if (sims.isNotEmpty())
                        simRepository.update(sims)
                }

                dataStore.edit {
                    it[PreferencesKeys.CURRENT_PACKAGES_VERSION] = DataPackages.PACKAGES_VERSION
                }
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
            (simManager.getDefaultSimBoth(SimType.VOICE)
                ?.setupDate ?: 0L) > 0L
        } catch (e: IllegalStateException) {
            false
        }
    }

    @Throws(PackageNotConfiguredException::class, MissingPermissionException::class, UnprocessableRequestException::class)
    override suspend fun buyDataPackage(dataPackage: DataPackage, sim: Sim) {

        if (sim.packages.isEmpty()) {
            throw UnsupportedOperationException("The sim must be provide with relations")
        }

        if (!sim.packages.contains(dataPackage)) {
            throw PackageNotConfiguredException()
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

    override suspend fun confirmPurchaseFromSms(smsBody: String, simId: String) {
        // Solo se confirma la compra: los datos llegan con la sincronización y sumarlos aquí
        // los duplicaría.
        DataPackages.findPurchasedPackage(smsBody)?.let {
            purchasedPackagesManager.confirmPurchased(it.id, simId)
        }
    }

    private suspend fun buyDataPackageForUSSD(dataPackage: DataPackage, sim: Sim) {

        ussdHelper.sendUSSDRequestLegacy(buildUssd(dataPackage), false, sim)

        purchasedPackagesManager.newPurchased(
            dataPackage.id,
            sim.id,
            IDataPackageManager.ConnectionMode.USSD
        )
    }

    private fun buildUssd(dataPackage: DataPackage): String =
        DataPackages.buildPurchaseUssd(dataPackage)
}