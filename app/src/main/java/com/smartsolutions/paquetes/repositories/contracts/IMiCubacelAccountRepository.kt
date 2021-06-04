package com.smartsolutions.paquetes.repositories.contracts

import androidx.room.Query
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import kotlinx.coroutines.flow.Flow

interface IMiCubacelAccountRepository {

    suspend fun create(account: MiCubacelAccount)

    suspend fun create(accounts: List<MiCubacelAccount>)

    suspend fun createOrUpdate(account: MiCubacelAccount)

    suspend fun all(): List<MiCubacelAccount>

    fun flow(): Flow<List<MiCubacelAccount>>

    suspend fun get(id: String): MiCubacelAccount?

    suspend fun getByPhone(phone: String): MiCubacelAccount?

    suspend fun update(account: MiCubacelAccount)

    suspend fun update(accounts: List<MiCubacelAccount>)

    suspend fun delete(account: MiCubacelAccount)
}