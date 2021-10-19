package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class DataPackageRepository @Inject constructor(
    private val dataPackageDao: IDataPackageDao,
    private val simDao: ISimDao
) : IDataPackageRepository {

    private val dispatcher = Dispatchers.IO

    override suspend fun all(): List<DataPackage> = withContext(dispatcher) {
        dataPackageDao.all()
    }


    override fun flow(): Flow<List<DataPackage>> =
        dataPackageDao.flow().map { list ->
            list.forEach {
                transform(it)
            }

            return@map list
        }

    override suspend fun get(id: DataPackages.PackageId): DataPackage? = withContext(dispatcher) {
        dataPackageDao.get(id)?.apply {
            transform(this)
        }
    }

    override suspend fun getByNetwork(network: String): List<DataPackage> {
       return withContext(dispatcher){
           dataPackageDao.getByNetwork(network).map {
               transform(it)
           }
       }
    }

    override suspend fun create(dataPackage: DataPackage): Long = withContext(dispatcher) {
        dataPackageDao.create(dataPackage)
    }

    override suspend fun create(dataPackages: List<DataPackage>): List<Long> = withContext(dispatcher) {
        dataPackageDao.create(dataPackages)
    }

    override suspend fun createOrUpdate(dataPackages: List<DataPackage>) {
        withContext(dispatcher) {
            dataPackageDao.createOrUpdate(dataPackages)
        }
    }

    override suspend fun update(dataPackage: DataPackage): Int = withContext(dispatcher) {
        dataPackageDao.update(dataPackage)
    }

    override suspend fun update(dataPackages: List<DataPackage>): Int = withContext(dispatcher) {
        dataPackageDao.update(dataPackages)
    }

    override suspend fun delete(dataPackage: DataPackage): Int = withContext(dispatcher) {
        dataPackageDao.delete(dataPackage)
    }

    override suspend fun delete(dataPackages: List<DataPackage>): Int = withContext(dispatcher) {
        dataPackageDao.delete(dataPackages)
    }

    private suspend fun transform(dataPackage: DataPackage): DataPackage {
        val sims = mutableListOf<Sim>()

        sims.addAll(
            withContext(dispatcher) {
                simDao.getByNetwork(Networks.NETWORK_3G_4G)
            }
        )

        when (dataPackage.network) {
            Networks.NETWORK_3G,
            Networks.NETWORK_3G_4G -> {
                sims.addAll(
                    withContext(dispatcher) {
                        simDao.getByNetwork(Networks.NETWORK_3G)
                    }
                )
            }
            Networks.NETWORK_4G -> {
                sims.addAll(
                    withContext(dispatcher) {
                        simDao.getByNetwork(Networks.NETWORK_4G)
                    }
                )
            }
        }

        dataPackage.sims = sims

        return dataPackage
    }
}