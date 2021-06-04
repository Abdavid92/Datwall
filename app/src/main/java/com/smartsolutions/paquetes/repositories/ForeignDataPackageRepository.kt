package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.data.ISimDataPackageDao
import com.smartsolutions.paquetes.repositories.contracts.IDataPackageRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.SimDataPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ForeignDataPackageRepository @Inject constructor(
    private val dataPackageDao: IDataPackageDao,
    private val simDao: ISimDao,
    private val simDataPackageDao: ISimDataPackageDao
) : IDataPackageRepository {

    override fun getAll(): Flow<List<DataPackage>> =
        dataPackageDao.getAll().map { list ->
            list.forEach { dataPackage ->
                transform(dataPackage)
            }
            return@map list
        }

    override fun getBySimId(simId: String): Flow<List<DataPackage>> =
        dataPackageDao.getAll().map { list ->
            val simDataPackage = simDataPackageDao.bySimId(simId)

            val finalList = mutableListOf<DataPackage>()

            simDataPackage.forEach { simData ->
                list.firstOrNull { it.id == simData.dataPackageId }?.let {
                    it.ussd = simData.ussd
                    finalList.add(it)
                }
            }

            return@map finalList
        }

    override suspend fun get(id: String): DataPackage? =
        dataPackageDao.get(id)?.apply {
            transform(this)
        }

    override suspend fun get(id: String, simId: String): DataPackage? {
        val dataPackage = dataPackageDao.get(id)

        if (dataPackage != null) {
            simDataPackageDao.get(simId, id)?.let {
                dataPackage.ussd = it.ussd
                simDao.get(simId)?.let { sim ->
                    dataPackage.sims = listOf(sim)
                }
            }
        }

        return dataPackage
    }

    override suspend fun create(dataPackage: DataPackage): Long {
        val result = dataPackageDao.create(dataPackage)

        if (result != -1L) {
            createForeignRelations(dataPackage)
        }

        return result
    }

    override suspend fun create(dataPackages: List<DataPackage>): List<Long> {
        val result = dataPackageDao.create(dataPackages)

        if (result.isNotEmpty()) {
            dataPackages.forEach {
                createForeignRelations(it)
            }
        }

        return result
    }

    override suspend fun update(dataPackage: DataPackage): Int {
        val result = dataPackageDao.update(dataPackage)

        updateForeignRelations(dataPackage)

        return result
    }

    override suspend fun update(dataPackages: List<DataPackage>): Int {
        val result = dataPackageDao.update(dataPackages)

        dataPackages.forEach {
            updateForeignRelations(it)
        }
        return result
    }

    override suspend fun delete(dataPackage: DataPackage): Int =
        dataPackageDao.delete(dataPackage)

    override suspend fun delete(dataPackages: List<DataPackage>): Int =
        dataPackageDao.delete(dataPackages)

    private suspend fun createForeignRelations(dataPackage: DataPackage) {
        dataPackage.ussd?.let { ussd ->
            dataPackage.sims.forEach { sim ->
                simDao.create(sim)

                val simData = SimDataPackage(0, ussd, sim.id, dataPackage.id)
                simDataPackageDao.create(simData)
            }
        }
    }

    private suspend fun updateForeignRelations(dataPackage: DataPackage) {
        dataPackage.ussd?.let { ussd ->
            dataPackage.sims.forEach { sim ->
                if (simDao.get(sim.id) == null) {
                    simDao.create(sim)
                } else {
                    simDao.update(sim)
                }

                var simData = simDataPackageDao.get(sim.id, dataPackage.id)

                if (simData != null) {
                    simData.ussd = ussd
                    simDataPackageDao.update(simData)
                } else {
                    simData = SimDataPackage(0, ussd, sim.id, dataPackage.id)
                    simDataPackageDao.create(simData)
                }
            }
        }
    }

    private suspend inline fun transform(dataPackage: DataPackage): DataPackage {
        val simDataPackages = simDataPackageDao.byDataPackageId(dataPackage.id)

        val sims = mutableListOf<Sim>()

        simDataPackages.forEach { simDataPackage ->
            val sim = simDao.get(simDataPackage.simId)

            if (sim != null)
                sims.add(sim)
        }
        dataPackage.sims = sims

        return dataPackage
    }
}