package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.ITrafficDao
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import javax.inject.Inject

class TrafficRepository @Inject constructor(private val dao: ITrafficDao) : ITrafficRepository {

    override suspend fun create(traffic: Traffic) = dao.create(traffic)

    override suspend fun create(traffics: List<Traffic>) = dao.create(traffics)

    override suspend fun update(traffic: Traffic) = dao.update(traffic)

    override suspend fun update(traffics: List<Traffic>) = dao.update(traffics)

    override suspend fun delete(traffic: Traffic) = dao.delete(traffic)

    override suspend fun delete(traffics: List<Traffic>) = dao.delete(traffics)

    override fun getAllLiveData() = dao.getAllLiveData()

    override suspend fun getAll() = dao.getAll()

    override suspend fun getByUid(uid: Int, startTime: Long, endTime: Long) = dao.getByUid(uid, startTime, endTime)

    override suspend fun getByTime(startTime: Long, endTime: Long) = dao.getByTime(startTime, endTime)
}