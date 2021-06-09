package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.micubacel.models.DataType
import com.smartsolutions.paquetes.repositories.models.DataPackage

interface IUserDataBytesManager {

    suspend fun addDataBytes(dataPackage: DataPackage, simId: String)

    suspend fun addPromoBonus(simId: String)

    suspend fun registerTraffic(rxBytes: Long, txBytes: Long, isLte: Boolean)

    suspend fun synchronizeUserDataBytes(data: List<DataType>, simId: String)
}