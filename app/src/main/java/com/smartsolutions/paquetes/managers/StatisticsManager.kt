package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.DataBytes
import com.smartsolutions.paquetes.repositories.AppRepository
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import javax.inject.Inject

class StatisticsManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val networkUsageManager: NetworkUsageManager,
    private val appRepository: AppRepository
) {

    //Retorno un Pair porque recorde que es necesario obtener igualmente lo consumido de la bolsa al igual que lo restante. Pero la verdad
    //no tengo idea de como hacerlo. Hay que tener en cuenta que NetworkUsageManager tiene un sistema de cache. Asi que no hay problema en
    //pedir dos veces el consumo. Lo que no tengo idea es como obtener los valores de todos los dataType sin enredarse y de manera optima.
    //Si, podemos repetir codigo en cada uno de los tipos y retornar pero no seria optimo
    suspend fun remainingBagDaily(simIndex: Int): Pair<DataBytes, DataBytes> {
        val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

        if (!bagDaily.isExpired() && !bagDaily.isEmpty()) {
            val consumed = networkUsageManager.getUsageTotal(bagDaily.startTime, System.currentTimeMillis()).totalBytes.bytes
            var remaining = bagDaily.bytesLte - consumed

            if (remaining < 0)
                remaining = 0

            return Pair(DataBytes(consumed), DataBytes(remaining))
        }

        return Pair(DataBytes(0L), DataBytes(0L))
    }

    suspend fun remainingInternational(simIndex: Int): DataBytes {
        val international = userDataBytesRepository.byType(UserDataBytes.DataType.International, simIndex)

        if (!international.isExpired() && !international.isEmpty()) {
            val consumed = networkUsageManager.getUsageTotal(
                international.startTime,
                System.currentTimeMillis()
            )


        }

        return DataBytes(0L)
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