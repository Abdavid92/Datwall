package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_4G
import com.smartsolutions.paquetes.data.IDataPackageDao
import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SimRepository @Inject constructor(
    private val simDao: ISimDao,
    private val dataPackageDao: IDataPackageDao
) : ISimRepository {

    private val dispatcher = Dispatchers.IO

    override suspend fun create(sim: Sim) {
        withContext(dispatcher) {
            simDao.create(sim)
        }
    }

    override suspend fun create(sims: List<Sim>) {
        withContext(dispatcher) {
            simDao.create(sims)
        }
    }

    override suspend fun all(withRelations: Boolean): List<Sim> =
        if (withRelations)
            withContext(dispatcher) {
                simDao.all()
            }.map {
                transform(it)
            }
        else
            withContext(dispatcher) {
                simDao.all()
            }

    override fun flow(withRelations: Boolean): Flow<List<Sim>> =
        if (withRelations)
            simDao.flow().map { list ->
                list.forEach {
                    transform(it)
                }
                return@map list
            }
        else
            simDao.flow()

    override suspend fun get(id: String, withRelations: Boolean): Sim? =
        if (withRelations)
            withContext(dispatcher) {
                simDao.get(id)
            }?.apply {
                transform(this)
            }
        else
            withContext(dispatcher) {
                simDao.get(id)
            }

    override suspend fun update(sim: Sim): Int {
        return withContext(dispatcher) {
            simDao.update(sim)
        }
    }

    override suspend fun update(sims: List<Sim>): Int {
        return withContext(dispatcher) {
            simDao.update(sims)
        }
    }

    override suspend fun delete(sim: Sim) = withContext(dispatcher) {
        simDao.delete(sim)
    }

    private suspend inline fun transform(sim: Sim): Sim {
        val packages = mutableListOf<DataPackage>()

        when (sim.network) {
            NETWORK_4G -> {
                packages.addAll(withContext(dispatcher) {
                    dataPackageDao.getByNetwork(NETWORK_4G)
                }.filter { !it.deprecated })
            }
            NETWORK_3G_4G -> {
                packages.addAll(withContext(dispatcher) {
                    dataPackageDao.getByNetwork(NETWORK_3G_4G)
                }.filter { !it.deprecated })
                packages.addAll(withContext(dispatcher) {
                    dataPackageDao.getByNetwork(NETWORK_4G)
                })
            }
            NETWORK_3G -> {
                packages.addAll(withContext(dispatcher) {
                    dataPackageDao.getByNetwork(NETWORK_3G_4G)
                }.filter { !it.deprecated })
                packages.addAll(withContext(dispatcher) {
                    dataPackageDao.getByNetwork(NETWORK_3G)
                }.filter { !it.deprecated })
            }
        }

        sim.packages = packages

        return sim
    }
}