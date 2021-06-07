package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.IPurchasedPackageDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.repositories.contracts.IPurchasedPackageRepository
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class PurchasedPackageRepository @Inject constructor(
    private val purchasedPackageDao: IPurchasedPackageDao,
    private val dataPackageDao: IDataPackageDao,
    private val simDao: ISimDao
): IPurchasedPackageRepository {

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
        purchasedPackageDao.getBySimId(simId).map {
            return@map transform(it)
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

    override fun getByDataPackageId(dataPackageId: String): Flow<List<PurchasedPackage>> =
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

    override fun getPending(dataPackageId: String): Flow<List<PurchasedPackage>> =
        purchasedPackageDao.getPending(dataPackageId).map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override suspend fun create(purchasedPackage: PurchasedPackage) = purchasedPackageDao.create(purchasedPackage)

    override suspend fun create(purchasedPackages: List<PurchasedPackage>) = purchasedPackageDao.create(purchasedPackages)

    override suspend fun update(purchasedPackage: PurchasedPackage) = purchasedPackageDao.update(purchasedPackage)

    override suspend fun update(purchasedPackages: List<PurchasedPackage>) = purchasedPackageDao.update(purchasedPackages)

    override suspend fun delete(purchasedPackage: PurchasedPackage) = purchasedPackageDao.delete(purchasedPackage)

    override suspend fun delete(purchasedPackages: List<PurchasedPackage>) = purchasedPackageDao.delete(purchasedPackages)

    private suspend fun transform(purchasedPackage: PurchasedPackage): PurchasedPackage {
        simDao.get(purchasedPackage.simId)?.let {
            purchasedPackage.sim = it
        }
        dataPackageDao.get(purchasedPackage.dataPackageId)?.let {
            purchasedPackage.dataPackage = it
        }
        return purchasedPackage
    }
}