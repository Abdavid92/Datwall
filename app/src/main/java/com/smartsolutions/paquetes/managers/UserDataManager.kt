package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IUserDataManager
import com.smartsolutions.paquetes.repositories.contracts.IUsageGeneralRepository
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserDataManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val usageGeneralRepository: IUsageGeneralRepository
) : IUserDataManager {

    override fun flowUserData(simId: String) = userDataBytesRepository.flowBySimId(simId)

    override suspend fun get(simId: String, type: DataBytes.DataType) = withContext(Dispatchers.IO) {
        userDataBytesRepository.get(simId, type)
    }

    override suspend fun save(userDataBytes: UserDataBytes) = withContext(Dispatchers.IO) {
        userDataBytesRepository.update(userDataBytes)
    }

    override suspend fun usage(simId: String, type: DataBytes.DataType, start: Long, finish: Long) =
        withContext(Dispatchers.IO) {
            usageGeneralRepository.inRangeTime(start, finish)
                .filter { it.simId == simId && it.type == type }
        }
}
