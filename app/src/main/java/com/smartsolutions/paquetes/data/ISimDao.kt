package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.flow.Flow

@Dao
interface ISimDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(sim: Sim)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(sims: List<Sim>)

    @Query("SELECT * FROM sims")
    suspend fun all(): List<Sim>

    @Query("SELECT * FROM sims")
    fun flow(): Flow<List<Sim>>

    @Query("SELECT * FROM sims WHERE id = :id")
    suspend fun get(id: String): Sim?

    @Query("SELECT * FROM sims WHERE network = :network")
    suspend fun getByNetwork(@Networks network: String): List<Sim>

    @Update
    suspend fun update(sim: Sim): Int

    @Update
    suspend fun update(sims: List<Sim>): Int

    @Delete
    suspend fun delete(sim: Sim): Int
}