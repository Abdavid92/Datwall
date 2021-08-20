package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject

class PurchasedPackagesManager @Inject constructor(
    private val purchasedPackageRepository: IPurchasedPackageRepository
) : IPurchasedPackagesManager {

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
        purchasedPackageRepository.create(purchasedPackage)
    }

    override suspend fun confirmPurchased(dataPackageId: DataPackages.PackageId, simId: String) {
        val pending = purchasedPackageRepository
            .getPending(dataPackageId)
            .first()
            .toMutableList()

        if (pending.isNotEmpty()) {

            val pendingToDelete = mutableListOf<PurchasedPackage>()

            pending.forEach {
                if (System.currentTimeMillis() - it.date > DateUtils.MILLIS_PER_DAY) {
                    purchasedPackageRepository.delete(it)
                    pendingToDelete.add(it)
                }
            }

            pending.removeAll(pendingToDelete)

            if (pending.isNotEmpty()) {
                val pendingToConfirmed = pending.firstOrNull { it.simId == simId }

                if (pendingToConfirmed != null) {
                    pendingToConfirmed.pending = false
                    purchasedPackageRepository.update(pendingToConfirmed)
                } else {
                    newPurchased(
                        dataPackageId,
                        simId,
                        IDataPackageManager.ConnectionMode.Unknown,
                        false
                    )
                }
            }
        }
    }

    override fun getHistory(): Flow<List<PurchasedPackage>> =
        purchasedPackageRepository.getAll()

    override suspend fun clearHistory() {
        purchasedPackageRepository
            .getAll()
            .firstOrNull()?.let {
                purchasedPackageRepository.delete(it)
            }
    }
}