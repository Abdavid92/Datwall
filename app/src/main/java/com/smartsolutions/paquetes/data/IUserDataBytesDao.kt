package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.flow.Flow

@Dao
interface IUserDataBytesDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(userDataBytes: UserDataBytes)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(userDataBytesList: List<UserDataBytes>)

    @Query("SELECT * FROM users_data_bytes")
    suspend fun all(): List<UserDataBytes>

    @Query("SELECT * FROM users_data_bytes")
    fun flow(): Flow<List<UserDataBytes>>

    @Query("SELECT * FROM users_data_bytes WHERE sim_id = :simId")
    suspend fun bySimId(simId: String): List<UserDataBytes>

    @Query("SELECT * FROM users_data_bytes WHERE sim_id = :simId")
    fun flowBySimId(simId: String): Flow<List<UserDataBytes>>

    @Query("SELECT * FROM users_data_bytes WHERE sim_id = :simId AND type = :type")
    suspend fun get(simId: String, type: String): UserDataBytes?

    @Update
    suspend fun update(userDataBytes: UserDataBytes)

    @Update
    suspend fun update(userDataBytesList: List<UserDataBytes>)

    @Delete
    suspend fun delete(userDataBytes: UserDataBytes)
}