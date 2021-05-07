package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import javax.inject.Inject

class StatisticsManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val networkUsageManager: NetworkUsageManager
) {

    suspend fun remainingBagDaily(simIndex: Int): DataBytes {
        val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

        if (bagDaily.isExpired() || bagDaily.isEmpty())
            return DataBytes(0)

        val traffic = networkUsageManager.getUsageTotal(bagDaily.startTime, System.currentTimeMillis())

        val remaining = bagDaily.bytesLte - traffic.totalBytes.bytes

        if (remaining < 0)
            return DataBytes(0)
        return DataBytes(remaining)
    }

    suspend fun remainingInternational(simIndex: Int): DataBytes {
        val international = userDataBytesRepository.byType(UserDataBytes.DataType.International, simIndex)

        if (international.isExpired() || international.isEmpty())
            return DataBytes(0)

        val consumed = networkUsageManager.getUsageTotal(international.startTime, System.currentTimeMillis())
        TODO("Not yet implemented")
    }

    suspend fun remainingNational(simIndex: Int): DataBytes {
        TODO("Not yet implemented")
    }

    suspend fun remainingBonus(simIndex: Int): DataBytes {
        TODO("Not yet implemented")
    }

    suspend fun remainingPromoBonus(simIndex: Int): DataBytes {
        TODO("Not yet implemented")
    }
}