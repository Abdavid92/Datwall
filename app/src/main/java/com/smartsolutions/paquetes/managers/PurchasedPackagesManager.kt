package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.DateUtils
import javax.inject.Inject

class PurchasedPackagesManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val purchasedPackageRepository: IPurchasedPackageRepository,
    private val simDelegate: SimDelegate
) : IPurchasedPackagesManager {

    private val dataStore by lazy { context.internalDataStore }

    override suspend fun newPurchased(
        dataPackageId: DataPackages.PackageId,
        simId: String,
        buyMode: IDataPackageManager.ConnectionMode,
        pending: Boolean
    ) {
        val purchasedPackage = PurchasedPackage(
            0,
            System.currentTimeMillis(),
            buyMode,
            simId,
            pending,
            dataPackageId
        )
        withContext(Dispatchers.IO) {
            purchasedPackageRepository.create(purchasedPackage)
        }
    }

    override suspend fun confirmPurchased(dataPackageId: DataPackages.PackageId, simId: String) {
        val pending = withContext(Dispatchers.IO) {
            purchasedPackageRepository.getPending(dataPackageId)
        }.first()

        PendingPurchaseSelector.select(pending, dataPackageId, simId, System.currentTimeMillis())
            ?.let {
                it.pending = false
                withContext(Dispatchers.IO) {
                    purchasedPackageRepository.update(it)
                }
            }
    }

    override fun getHistory(): Flow<List<PurchasedPackage>> =
        purchasedPackageRepository.getAll()

    override suspend fun clearHistory() {
        withContext(Dispatchers.IO) {
            purchasedPackageRepository
                .getAll()
        }.firstOrNull()
            // Las compras sin confirmar se conservan para poder confirmarlas después.
            ?.filter { !it.pending }
            ?.let {
                withContext(Dispatchers.IO) {
                    purchasedPackageRepository.delete(it)
                }
            }
    }


}