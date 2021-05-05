package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject

class PurchasedPackagesManager @Inject constructor(
    private val purchasedPackageRepository: IPurchasedPackageRepository
) {

    suspend fun newPurchased(dataPackageId: String, simIndex: Int, buyMode: IDataPackageManager.BuyMode) {
        val purchasedPackage = PurchasedPackage(
            0,
            System.currentTimeMillis(),
            buyMode,
            simIndex,
            true,
            dataPackageId
        )
        purchasedPackageRepository.create(purchasedPackage)
    }

    suspend fun confirmPurchased(dataPackageId: String) {
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
                pending[0].pending = false
                purchasedPackageRepository.update(pending[0])
            }
        }
    }

    fun getHistory(): Flow<List<PurchasedPackage>> =
        purchasedPackageRepository.getAll()

    suspend fun clearHistory() {
        purchasedPackageRepository
            .getAll()
            .firstOrNull()?.let {
                purchasedPackageRepository.delete(it)
            }
    }
}