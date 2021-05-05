package com.smartsolutions.paquetes.repositories.contracts

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.repositories.models.UserDataBytes

interface IUserDataBytesRepository {

    fun getAll(): LiveData<List<UserDataBytes>>

    suspend fun all(): List<UserDataBytes>

    suspend fun getBySimIndex(simIndex: Int): List<UserDataBytes>

    suspend fun byType(dataType: UserDataBytes.DataType, simIndex: Int): UserDataBytes?

    suspend fun create(userDataBytes: UserDataBytes): Boolean

    suspend fun create(userDataBytesList: List<UserDataBytes>): Boolean

    suspend fun update(userDataBytes: UserDataBytes): Boolean

    suspend fun update(userDataBytesList: List<UserDataBytes>): Boolean

    suspend fun delete(userDataBytes: UserDataBytes): Boolean
}