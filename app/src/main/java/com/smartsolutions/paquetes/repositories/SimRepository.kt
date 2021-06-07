package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_4G
import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.IMiCubacelAccountDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SimRepository @Inject constructor(
    private val simDao: ISimDao,
    private val dataPackageDao: IDataPackageDao,
    private val miCubacelAccountDao: IMiCubacelAccountDao
) : ISimRepository {

    override suspend fun create(sim: Sim) {
        simDao.create(sim)
    }

    override suspend fun create(sims: List<Sim>) {
        simDao.create(sims)
    }

    override suspend fun all(): List<Sim> =
        simDao.all()

    override fun flow(): Flow<List<Sim>> =
        simDao.flow()

    override suspend fun get(id: String): Sim? =
        simDao.get(id)?.apply {
            transform(this)
        }

    override suspend fun update(sim: Sim): Int {
        return simDao.update(sim)
    }

    override suspend fun update(sims: List<Sim>): Int {
      return simDao.update(sims)
    }

    override suspend fun delete(sim: Sim) = simDao.delete(sim)

    private suspend inline fun transform(sim: Sim): Sim {
        val packages = mutableListOf<DataPackage>()

        when (sim.network) {
            NETWORK_4G -> {
                packages.addAll(dataPackageDao.getByNetwork(NETWORK_4G))
            }
            NETWORK_3G_4G -> {
                packages.addAll(dataPackageDao.getByNetwork(NETWORK_3G_4G))
                packages.addAll(dataPackageDao.getByNetwork(NETWORK_4G))
            }
            NETWORK_3G -> {
                packages.addAll(dataPackageDao.getByNetwork(NETWORK_3G_4G))
                packages.addAll(dataPackageDao.getByNetwork(NETWORK_3G))
            }
        }

        sim.packages = packages

        miCubacelAccountDao.get(sim.id)?.let {
            sim.miCubacelAccount = it
        }

        return sim
    }
}