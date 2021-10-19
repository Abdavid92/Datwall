package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.IPurchasedPackageDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PurchasedPackageRepository @Inject constructor(
    private val purchasedPackageDao: IPurchasedPackageDao,
    private val dataPackageDao: IDataPackageDao,
    private val simDao: ISimDao
) : IPurchasedPackageRepository {

    private val dispatcher = Dispatchers.IO

    override fun getAll(): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.getAll().map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override fun getByDate(start: Long, finish: Long): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.getByDate(start, finish).map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override suspend fun getBySimId(simId: String): List<PurchasedPackage> =
        withContext(dispatcher) {
            purchasedPackageDao.getBySimId(simId).map {
                return@map transform(it)
            }
        }

    override fun flowBySimId(simId: String): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.flowBySimId(simId).map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override fun get(id: Long): Flow<PurchasedPackage> =
        purchasedPackageDao.get(id).map {
            return@map transform(it)
        }

    override fun getByDataPackageId(dataPackageId: DataPackages.PackageId): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.getByDataPackageId(dataPackageId).map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override fun getPending(): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.getPending().map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override fun getPending(dataPackageId: DataPackages.PackageId): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.getPending(dataPackageId).map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override suspend fun create(purchasedPackage: PurchasedPackage) = withContext(dispatcher) {
        purchasedPackageDao.create(purchasedPackage)
    }

    override suspend fun create(purchasedPackages: List<PurchasedPackage>) =
        withContext(dispatcher) {
            purchasedPackageDao.create(purchasedPackages)
        }

    override suspend fun update(purchasedPackage: PurchasedPackage) = withContext(dispatcher) {
        purchasedPackageDao.update(purchasedPackage)
    }

    override suspend fun update(purchasedPackages: List<PurchasedPackage>) =
        withContext(dispatcher) {
            purchasedPackageDao.update(purchasedPackages)
        }

    override suspend fun delete(purchasedPackage: PurchasedPackage) = withContext(dispatcher) {
        purchasedPackageDao.delete(purchasedPackage)
    }

    override suspend fun delete(purchasedPackages: List<PurchasedPackage>) =
        withContext(dispatcher) {
            purchasedPackageDao.delete(purchasedPackages)
        }

    private suspend fun transform(purchasedPackage: PurchasedPackage): PurchasedPackage {
        withContext(dispatcher) {
            simDao.get(purchasedPackage.simId)
        }?.let {
            purchasedPackage.sim = it
        }
        withContext(dispatcher) {
            dataPackageDao.get(purchasedPackage.dataPackageId)
        }?.let {
            purchasedPackage.dataPackage = it
        }
        return purchasedPackage
    }
}