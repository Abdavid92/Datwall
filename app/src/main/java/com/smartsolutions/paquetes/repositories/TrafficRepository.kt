package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.ITrafficDao
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TrafficRepository @Inject constructor(private val dao: ITrafficDao) : ITrafficRepository {

    override suspend fun create(traffic: Traffic) = dao.create(traffic)

    override suspend fun create(traffics: List<Traffic>) = dao.create(traffics)

    override suspend fun update(traffic: Traffic) = dao.update(traffic)

    override suspend fun update(traffics: List<Traffic>) = dao.update(traffics)

    override suspend fun delete(traffic: Traffic) = dao.delete(traffic)

    override suspend fun delete(traffics: List<Traffic>) = dao.delete(traffics)

    override fun getFlow(simId: String) = dao.getFlow(simId)

    override suspend fun getAll(simId: String) = dao.getAll(simId)

    override suspend fun getByUid(uid: Int, simId: String, startTime: Long, endTime: Long) =
        dao.getByUid(uid, simId, startTime, endTime)

    override fun getFlowByUid(uid: Int, simId: String, startTime: Long, endTime: Long): Flow<List<Traffic>> =
        dao.getFlowByUid(uid, simId, startTime, endTime)

    override suspend fun getByTime(simId: String, startTime: Long, endTime: Long) =
        dao.getByTime(simId, startTime, endTime)

    override fun getFlowByTime(simId: String, startTime: Long, endTime: Long): Flow<List<Traffic>> =
        dao.getFlowByTime(simId, startTime, endTime)
}