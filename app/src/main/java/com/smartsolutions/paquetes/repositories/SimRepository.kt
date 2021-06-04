package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.data.ISimDataPackageDao
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.SimDataPackage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SimRepository @Inject constructor(
    private val simDao: ISimDao,
    private val simDataPackageDao: ISimDataPackageDao,
    private val dataPackageDao: IDataPackageDao
) : ISimRepository {

    override suspend fun create(sim: Sim) {
        simDao.create(sim)
        createForeignRelations(sim)
    }

    override suspend fun create(sims: List<Sim>) {
        simDao.create(sims)

        sims.forEach {
            createForeignRelations(it)
        }
    }

    override suspend fun all(): List<Sim> =
        simDao.all().map { sim ->
            return@map transform(sim)
        }

    override fun flow(): Flow<List<Sim>> =
        simDao.flow().map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override suspend fun get(id: String): Sim? =
        simDao.get(id)?.apply {
            transform(this)
        }

    override suspend fun update(sim: Sim): Int {
        val result = simDao.update(sim)

        updateForeignRelations(sim)

        return result
    }

    override suspend fun update(sims: List<Sim>): Int {
        val result = simDao.update(sims)

        sims.forEach {
            updateForeignRelations(it)
        }

        return result
    }

    override suspend fun delete(sim: Sim) = simDao.delete(sim)

    private suspend fun createForeignRelations(sim: Sim) {
        sim.packages.forEach { dataPackage ->
            dataPackage.ussd?.let { ussd ->
                dataPackageDao.create(dataPackage)

                val simData = SimDataPackage(0, ussd, sim.id, dataPackage.id)
                simDataPackageDao.create(simData)
            }
        }
    }

    private suspend fun updateForeignRelations(sim: Sim) {
        sim.packages.forEach { dataPackage ->
            dataPackage.ussd?.let { ussd ->
                if (dataPackageDao.get(dataPackage.id) == null) {
                    dataPackageDao.create(dataPackage)
                } else {
                    dataPackageDao.update(dataPackage)
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

    private suspend inline fun transform(sim: Sim): Sim {
        val simDataPackages = simDataPackageDao.bySimId(sim.id)
        val packages = mutableListOf<DataPackage>()

        simDataPackages.forEach { simDataPackage ->
            val dataPackage = dataPackageDao.get(simDataPackage.dataPackageId)

            if (dataPackage != null) {
                dataPackage.ussd = simDataPackage.ussd
                packages.add(dataPackage)
            }
        }

        sim.packages = packages

        return sim
    }
}