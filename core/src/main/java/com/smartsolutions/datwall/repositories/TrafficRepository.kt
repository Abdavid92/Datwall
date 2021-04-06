package com.smartsolutions.datwall.repositories

import androidx.lifecycle.LiveData
import com.smartsolutions.datwall.data.ITrafficDao
import com.smartsolutions.datwall.managers.models.Traffic
import javax.inject.Inject

class TrafficRepository @Inject constructor(private val dao: ITrafficDao) {

    suspend fun create(traffic: Traffic) = dao.create(traffic)

    suspend fun create(traffics: List<Traffic>) = dao.create(traffics)

    suspend fun update(traffic: Traffic) = dao.update(traffic)

    suspend fun update(traffics: List<Traffic>) = dao.update(traffics)

    suspend fun delete(traffic: Traffic) = dao.delete(traffic)

    suspend fun delete(traffics: List<Traffic>) = dao.delete(traffics)

    fun getAllLiveData() = dao.getAllLiveData()

    suspend fun getAll() = dao.getAll()

    suspend fun getByUid(uid: Int, startTime: Long, endTime: Long) = dao.getByUid(uid, startTime, endTime)

    suspend fun getByTime(startTime: Long, endTime: Long) = dao.getByTime(startTime, endTime)
}