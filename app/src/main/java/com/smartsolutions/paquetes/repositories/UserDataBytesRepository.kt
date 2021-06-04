package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.ISimDao
import com.smartsolutions.paquetes.data.IUserDataBytesDao
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

class UserDataBytesRepository @Inject constructor(
    private val userDataBytesDao: IUserDataBytesDao,
    private val simDao: ISimDao
): IUserDataBytesRepository {

    override suspend fun all(): List<UserDataBytes> =
        userDataBytesDao.all().map {
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
        val userDataBytesList: MutableList<UserDataBytes> = userDataBytesDao
            .bySimId(simId).toMutableList()

        if (userDataBytesList.size < 5) {
            seedUserDataBytes(simId, userDataBytesList)
        }

        return userDataBytesList.map {
            transform(it)
        }
    }

    override fun flowBySimId(simId: String): Flow<List<UserDataBytes>> =
        userDataBytesDao.flowBySimId(simId).map { list ->
            if (list.size < 5) {
                return@map seedUserDataBytes(simId, list.toMutableList()).map {
                    transform(it)
                }
            }
            return@map list
        }

    override suspend fun get(simId: String, type: UserDataBytes.DataType): UserDataBytes? {
        var userDataBytes = userDataBytesDao.get(simId, type.name)

        if (userDataBytes == null) {
            userDataBytes = UserDataBytes(
                simId,
                type,
                0,
                0,
                0,
                0,
                0
            )

            userDataBytesDao.create(userDataBytes)
        }

        return transform(userDataBytes)
    }

    override suspend fun update(userDataBytes: UserDataBytes) =
        userDataBytesDao.update(userDataBytes)

    override suspend fun update(userDataBytesList: List<UserDataBytes>) =
        userDataBytesDao.update(userDataBytesList)

    override suspend fun delete(userDataBytes: UserDataBytes) =
        userDataBytesDao.delete(userDataBytes)

    private suspend fun transform(userDataBytes: UserDataBytes): UserDataBytes {
        simDao.get(userDataBytes.simId)?.let {
            userDataBytes.sim = it
        }
        return userDataBytes
    }

    private suspend fun seedUserDataBytes(simId: String, userDataBytesList: MutableList<UserDataBytes>): List<UserDataBytes> {
        UserDataBytes.DataType.values().forEach { dataType ->
            if (userDataBytesList.firstOrNull { it.type == dataType} == null) {

                val userDataBytes = UserDataBytes(
                    simId,
                    dataType,
                    0,
                    0,
                    0,
                    0,
                    0
                )

                userDataBytesDao.create(userDataBytes)
                userDataBytesList.add(userDataBytes)
            }
        }
        return userDataBytesList
    }
}