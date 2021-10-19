package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.data.IUserDataBytesDao
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserDataBytesRepository @Inject constructor(
    private val userDataBytesDao: IUserDataBytesDao,
    private val simDao: ISimDao
) : IUserDataBytesRepository {

    private val dispatcher = Dispatchers.IO

    override suspend fun all(): List<UserDataBytes> =
        withContext(dispatcher) {
            userDataBytesDao.all()
        }.map {
            transform(it)
        }

    override fun flow(): Flow<List<UserDataBytes>> =
        userDataBytesDao.flow().map { list ->
            list.forEach {
                transform(it)
            }
            return@map list
        }

    override suspend fun bySimId(simId: String): List<UserDataBytes> {
        try {
            val userDataBytesList: MutableList<UserDataBytes> = withContext(dispatcher) {
                userDataBytesDao.bySimId(simId).toMutableList()
            }

            if (userDataBytesList.size < DataType.values().size) {
                seedUserDataBytes(simId, userDataBytesList)
            }

            return userDataBytesList.map {
                transform(it)
            }
        } catch (e: Exception) {

        }
        return emptyList()
    }

    override fun flowBySimId(simId: String): Flow<List<UserDataBytes>> =
        userDataBytesDao.flowBySimId(simId)
            .map { list ->
                if (list.size != DataType.values().size) {
                    return@map seedUserDataBytes(simId, list.toMutableList()).map {
                        transform(it)
                    }
                }
                return@map list
            }.catch {
                emit(emptyList())
            }

    override suspend fun get(simId: String, type: DataType): UserDataBytes {
        var userDataBytes = withContext(dispatcher) {
            userDataBytesDao.get(simId, type.name)
        }

        if (userDataBytes == null) {
            userDataBytes = UserDataBytes(
                simId,
                type,
                0,
                0,
                0,
                0
            )

            withContext(dispatcher) {
                userDataBytesDao.create(userDataBytes)
            }
        }

        return transform(userDataBytes)
    }

    override suspend fun update(userDataBytes: UserDataBytes) =
        withContext(dispatcher) {
            userDataBytesDao.update(userDataBytes)
        }

    override suspend fun update(userDataBytesList: List<UserDataBytes>) =
        withContext(dispatcher) {
            userDataBytesDao.update(userDataBytesList)
        }

    override suspend fun delete(userDataBytes: UserDataBytes) =
        withContext(dispatcher) {
            userDataBytesDao.delete(userDataBytes)
        }

    private suspend fun transform(userDataBytes: UserDataBytes): UserDataBytes {
        withContext(dispatcher) {
            simDao.get(userDataBytes.simId)
        }?.let {
            userDataBytes.sim = it
        }
        return userDataBytes
    }

    private suspend fun seedUserDataBytes(
        simId: String,
        userDataBytesList: MutableList<UserDataBytes>
    ): List<UserDataBytes> {
        DataType.values().forEach { dataType ->
            if (userDataBytesList.firstOrNull { it.type == dataType } == null) {

                val userDataBytes = UserDataBytes(
                    simId,
                    dataType,
                    0,
                    0,
                    0,
                    0
                )

                withContext(dispatcher){
                    userDataBytesDao.create(userDataBytes)
                }
                userDataBytesList.add(userDataBytes)
            }
        }
        return userDataBytesList
    }
}