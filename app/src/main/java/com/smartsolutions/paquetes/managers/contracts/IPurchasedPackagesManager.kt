package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow

interface IPurchasedPackagesManager {
    suspend fun newPurchased(
        dataPackageId: DataPackages.PackageId,
        simId: String,
        buyMode: IDataPackageManager.ConnectionMode,
        pending: Boolean = true
    )

    suspend fun confirmPurchased(dataPackageId: DataPackages.PackageId, simId: String)

    fun getHistory(): Flow<List<PurchasedPackage>>

    suspend fun clearHistory()

    suspend fun seedOldPurchasedPackages()
}