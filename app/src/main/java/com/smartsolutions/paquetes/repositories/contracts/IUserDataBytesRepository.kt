package com.smartsolutions.paquetes.repositories.contracts

import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.flow.Flow

interface IUserDataBytesRepository {

    suspend fun all(): List<UserDataBytes>

    fun flow(): Flow<List<UserDataBytes>>

    suspend fun bySimId(simId: String): List<UserDataBytes>

    fun flowBySimId(simId: String): Flow<List<UserDataBytes>>

    suspend fun get(simId: String, type: UserDataBytes.DataType): UserDataBytes?

    suspend fun update(userDataBytes: UserDataBytes)

    suspend fun update(userDataBytesList: List<UserDataBytes>)

    suspend fun delete(userDataBytes: UserDataBytes)
}