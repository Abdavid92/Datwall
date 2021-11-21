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

    @Query("SELECT * FROM traffic WHERE sim_id = :simID")
    suspend fun getAll(simID: String): List<Traffic>

    @Query("SELECT * FROM traffic WHERE sim_id = :simID")
    fun getFlow(simID: String): Flow<List<Traffic>>

    @Query("SELECT * FROM traffic WHERE uid = :uid AND sim_id = :simID AND start_time >= :startTime AND end_time <= :endTime")
    suspend fun getByUid(uid: Int, simID: String, startTime: Long, endTime: Long): List<Traffic>

    @Query("SELECT * FROM traffic WHERE uid = :uid AND sim_id = :simID AND start_time >= :startTime AND end_time <= :endTime")
    fun getFlowByUid(uid: Int, simID: String, startTime: Long, endTime: Long): Flow<List<Traffic>>

    @Query("SELECT * FROM traffic WHERE sim_id = :simID AND start_time >= :startTime AND end_time <= :endTime ORDER BY :startTime")
    suspend fun getByTime(simID: String, startTime: Long, endTime: Long): List<Traffic>

    @Query("SELECT * FROM traffic WHERE sim_id = :simID AND start_time >= :startTime AND end_time <= :endTime")
    fun getFlowByTime(simID: String, startTime: Long, endTime: Long): Flow<List<Traffic>>
}