package com.smartsolutions.paquetes.repositories.contracts

import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import kotlinx.coroutines.flow.Flow

interface IMiCubacelAccountRepository {

    suspend fun create(account: MiCubacelAccount): Boolean

    suspend fun update(account: MiCubacelAccount): Boolean

    suspend fun createOrUpdate(account: MiCubacelAccount): Boolean

    suspend fun delete(account: MiCubacelAccount): Boolean

    fun get(phone: String): Flow<MiCubacelAccount>

    fun get(simIndex: Int): Flow<MiCubacelAccount>

    fun getAll(): Flow<List<MiCubacelAccount>>

}