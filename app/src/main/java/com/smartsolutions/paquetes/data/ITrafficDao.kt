package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.managers.models.Traffic
import kotlinx.coroutines.flow.Flow

@Dao
interface ITrafficDao {

    @Insert
    suspend fun create(traffic: Traffic): Long

    @Insert
    suspend fun create(traffics: List<Traffic>): List<Long>

    @Update
    suspend fun update(traffic: Traffic): Int

    @Update
    suspend fun update(traffics: List<Traffic>): Int

    @Delete
    suspend fun delete(traffic: Traffic): Int

    @Delete
    suspend fun delete(traffics: List<Traffic>): Int

    @Query("SELECT * FROM traffic WHERE sim_id = :simID ORDER BY start_time")
    suspend fun getAll(simID: String): List<Traffic>

    @Query("SELECT * FROM traffic WHERE sim_id = :simID")
    fun getFlow(simID: String): Flow<List<Traffic>>

    @Query("SELECT * FROM traffic WHERE uid = :uid AND sim_id = :simID")
    suspend fun getByUid(uid: Int, simID: String): List<Traffic>

    @Query("SELECT * FROM traffic WHERE uid = :uid AND sim_id = :simID")
    fun getFlowByUid(uid: Int, simID: String): Flow<List<Traffic>>

    @Query("SELECT * FROM traffic WHERE sim_id = :simID ORDER BY start_time")
    suspend fun getByTime(simID: String): List<Traffic>

    @Query("SELECT * FROM traffic WHERE sim_id = :simID")
    fun getFlowByTime(simID: String, startTime: Long, endTime: Long): Flow<List<Traffic>>
}