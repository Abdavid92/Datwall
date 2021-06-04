package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_4G
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
    private val dataPackageDao: IDataPackageDao
) : ISimRepository {

    override suspend fun create(sim: Sim) {
        simDao.create(sim)
    }

    override suspend fun create(sims: List<Sim>) {
        simDao.create(sims)
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
        return simDao.update(sim)
    }

    override suspend fun update(sims: List<Sim>): Int {
      return simDao.update(sims)
    }

    override suspend fun delete(sim: Sim) = simDao.delete(sim)

    private suspend inline fun transform(sim: Sim): Sim {
        val packages = mutableListOf<DataPackage>()

        when (sim.networks.toString()) {
            NETWORK_4G -> {
                dataPackageDao.getByNetwork(NETWORK_4G).forEach {
                    packages.add(it)
                }
            }
            NETWORK_3G_4G -> {
                dataPackageDao.getByNetwork(NETWORK_3G_4G).forEach {
                    packages.add(it)
                }
                dataPackageDao.getByNetwork(NETWORK_4G).forEach {
                    packages.add(it)
                }
            }
            else -> {
                dataPackageDao.getByNetwork(NETWORK_3G_4G).forEach {
                    packages.add(it)
                }
            }
        }

        sim.packages = packages

        return sim
    }
}