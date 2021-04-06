package com.smartsolutions.datwall.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smartsolutions.datwall.managers.models.Traffic
import retrofit2.http.GET
import java.util.*

@Dao
interface ITrafficDao {

    @Insert
    suspend fun create(traffic: Traffic) : Long

    @Insert
    suspend fun create(traffics : List<Traffic>) : List<Long>

    @Update
    suspend fun update(traffic: Traffic) : Int

    @Update
    suspend fun update(traffics : List<Traffic>) : Int

    @Delete
    suspend fun delete(traffic : Traffic) : Int

    @Delete
    suspend fun delete(traffics: List<Traffic>) : Int

    @Query("SELECT * FROM traffic")
    fun getAllLiveData() : LiveData<List<Traffic>>

    @Query("SELECT * FROM traffic")
    suspend fun getAll() : List<Traffic>

    @Query("SELECT * FROM traffic WHERE uid = :uid AND start_time >= :startTime AND end_time <= :endTime")
    suspend fun getByUid(uid : Int, startTime : Long, endTime : Long) : List<Traffic>

    @Query("SELECT * FROM traffic WHERE start_time >= :startTime AND end_time <= :endTime")
    suspend fun getByTime(startTime: Long, endTime: Long) : List<Traffic>

}