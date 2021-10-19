package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.ITrafficDao
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class TrafficRepository @Inject constructor(private val dao: ITrafficDao) : ITrafficRepository {

    private val dispatcher = Dispatchers.IO

    override suspend fun create(traffic: Traffic) = withContext(dispatcher){
        dao.create(traffic)
    }

    override suspend fun create(traffics: List<Traffic>) = withContext(dispatcher){
        dao.create(traffics)
    }

    override suspend fun update(traffic: Traffic) = withContext(dispatcher){
        dao.update(traffic)
    }

    override suspend fun update(traffics: List<Traffic>) = withContext(dispatcher){
        dao.update(traffics)
    }

    override suspend fun delete(traffic: Traffic) = withContext(dispatcher){
        dao.delete(traffic)
    }

    override suspend fun delete(traffics: List<Traffic>) = withContext(dispatcher){
        dao.delete(traffics)
    }

    override fun getFlow(simId: String) = dao.getFlow(simId)

    override suspend fun getAll(simId: String) = withContext(dispatcher){
        dao.getAll(simId)
    }

    override suspend fun getByUid(uid: Int, simId: String, startTime: Long, endTime: Long) =
        withContext(dispatcher){
            dao.getByUid(uid, simId, startTime, endTime)
        }

    override fun getFlowByUid(uid: Int, simId: String, startTime: Long, endTime: Long): Flow<List<Traffic>> =
        dao.getFlowByUid(uid, simId, startTime, endTime)

    override suspend fun getByTime(simId: String, startTime: Long, endTime: Long) =
        withContext(dispatcher){
            dao.getByTime(simId, startTime, endTime)
        }

    override fun getFlowByTime(simId: String, startTime: Long, endTime: Long): Flow<List<Traffic>> =
        dao.getFlowByTime(simId, startTime, endTime)
}