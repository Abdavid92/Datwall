package com.smartsolutions.paquetes.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.IPurchasedPackageDao
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class PurchasedPackageRepository @Inject constructor(
    private val purchasedPackageDao: IPurchasedPackageDao,
    private val dataPackageDao: IDataPackageDao
): IPurchasedPackageRepository, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun getAll(): Flow<List<PurchasedPackage>> =
        onEach(purchasedPackageDao.getAll())

    override fun getByDate(start: Long, finish: Long): Flow<List<PurchasedPackage>> =
        onEach(purchasedPackageDao.getByDate(start, finish))

    override fun get(id: Long): Flow<PurchasedPackage> =
        purchasedPackageDao.get(id).onEach {
            it.dataPackage = dataPackageDao.get(it.dataPackageId)
        }

    override fun getByDataPackageId(dataPackageId: String): Flow<List<PurchasedPackage>> =
        onEach(purchasedPackageDao.getByDataPackageId(dataPackageId))

    override fun getPending(): Flow<List<PurchasedPackage>> =
        onEach(purchasedPackageDao.getPending())

    override fun getPending(dataPackageId: String): Flow<List<PurchasedPackage>> =
        onEach(purchasedPackageDao.getPending(dataPackageId))

    override suspend fun create(purchasedPackage: PurchasedPackage) = purchasedPackageDao.create(purchasedPackage)

    override suspend fun create(purchasedPackages: List<PurchasedPackage>) = purchasedPackageDao.create(purchasedPackages)

    override suspend fun update(purchasedPackage: PurchasedPackage) = purchasedPackageDao.update(purchasedPackage)

    override suspend fun update(purchasedPackages: List<PurchasedPackage>) = purchasedPackageDao.update(purchasedPackages)

    override suspend fun delete(purchasedPackage: PurchasedPackage) = purchasedPackageDao.delete(purchasedPackage)

    override suspend fun delete(purchasedPackages: List<PurchasedPackage>) = purchasedPackageDao.delete(purchasedPackages)

    private fun onEach(flow: Flow<List<PurchasedPackage>>) =
        flow.onEach {
            it.forEach { purchasedPackage ->
                purchasedPackage.dataPackage = dataPackageDao.get(purchasedPackage.dataPackageId)
            }
        }
}