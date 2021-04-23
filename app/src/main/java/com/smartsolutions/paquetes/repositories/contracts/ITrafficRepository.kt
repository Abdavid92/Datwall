package com.smartsolutions.paquetes.repositories.contracts

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.managers.models.Traffic

interface ITrafficRepository {
    suspend fun create(traffic: Traffic): Long

    suspend fun create(traffics: List<Traffic>): List<Long>

    suspend fun update(traffic: Traffic): Int

    suspend fun update(traffics: List<Traffic>): Int

    suspend fun delete(traffic: Traffic): Int

    suspend fun delete(traffics: List<Traffic>): Int

    fun getAllLiveData(): LiveData<List<Traffic>>

    suspend fun getAll(): List<Traffic>

    suspend fun getByUid(uid: Int, startTime: Long, endTime: Long): List<Traffic>

    suspend fun getByTime(startTime: Long, endTime: Long): List<Traffic>
}