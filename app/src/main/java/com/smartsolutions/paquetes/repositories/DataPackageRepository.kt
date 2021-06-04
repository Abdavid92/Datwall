package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class DataPackageRepository @Inject constructor(
    private val dataPackageDao: IDataPackageDao,
) : IDataPackageRepository {

    override fun getAll(): Flow<List<DataPackage>> = dataPackageDao.getAll()

    /*override fun getActives(simId: String): Flow<List<DataPackage>> {
        return dataPackageDao.getAll().map {
            val actives = mutableListOf<DataPackage>()
            it.forEach { dataPackage ->
                if (simId == 1 && dataPackage.activeInSim1)
                    actives.add(dataPackage)
                else if (simId == 2 && dataPackage.activeInSim2)
                    actives.add(dataPackage)
            }
            actives
        }
    }*/

    override fun getBySimId(simId: String): Flow<List<DataPackage>> {
        TODO("Not yet implemented")
    }

    override suspend fun get(id: String): DataPackage? = dataPackageDao.get(id)

    override suspend fun get(id: String, simId: String): DataPackage? {
        TODO("Not yet implemented")
    }

    override suspend fun create(dataPackage: DataPackage): Long = dataPackageDao.create(dataPackage)

    override suspend fun create(dataPackages: List<DataPackage>): List<Long> = dataPackageDao.create(dataPackages)

    override suspend fun update(dataPackage: DataPackage): Int = dataPackageDao.update(dataPackage)

    override suspend fun update(dataPackages: List<DataPackage>): Int = dataPackageDao.update(dataPackages)

    override suspend fun delete(dataPackage: DataPackage): Int = dataPackageDao.delete(dataPackage)

    override suspend fun delete(dataPackages: List<DataPackage>): Int = dataPackageDao.delete(dataPackages)
}