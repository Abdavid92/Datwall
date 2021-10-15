package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.UsageGeneral

@Dao
interface IUsageGeneralDao {

    @Query("SELECT * FROM usage_general")
    suspend fun getAll(): List<UsageGeneral>

    @Query("SELECT * FROM usage_general WHERE date <= :finish AND date >= :start")
    suspend fun inRangeTime(start: Long, finish: Long): List<UsageGeneral>

    @Insert
    suspend fun create(usageGeneral: UsageGeneral)

    @Insert
    suspend fun create(usageGeneral: List<UsageGeneral>)

    @Update
    suspend fun update(usageGeneral: UsageGeneral)

    @Update
    suspend fun update(usageGeneral: List<UsageGeneral>)

    @Delete
    suspend fun delete(usageGeneral: UsageGeneral)

    @Delete
    suspend fun delete(usageGeneral: List<UsageGeneral>)

}