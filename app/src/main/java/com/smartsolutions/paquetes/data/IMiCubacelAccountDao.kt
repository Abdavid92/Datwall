package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface IMiCubacelAccountDao {

    @Insert
    suspend fun create(account: MiCubacelAccount)

    @Insert
    suspend fun create(accounts: List<MiCubacelAccount>)

    @Query("SELECT * FROM mi_cubacel_accounts")
    suspend fun all(): List<MiCubacelAccount>

    @Query("SELECT * FROM mi_cubacel_accounts")
    fun flow(): Flow<List<MiCubacelAccount>>

    @Query("SELECT * FROM mi_cubacel_accounts WHERE sim_id = :id")
    fun get(id: String): MiCubacelAccount?

    @Query("SELECT * FROM mi_cubacel_accounts WHERE phone = :phone")
    fun getByPhone(phone: String): MiCubacelAccount?

    @Update
    suspend fun update(account: MiCubacelAccount)

    @Update
    suspend fun update(accounts: List<MiCubacelAccount>)

    @Delete
    suspend fun delete(account: MiCubacelAccount)
}