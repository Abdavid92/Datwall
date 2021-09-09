package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.DataPackage

interface IUserDataBytesManager {

    suspend fun addDataBytes(dataPackage: DataPackage, simId: String)

    suspend fun addPromoBonus(simId: String, bytes: Long)

    suspend fun registerTraffic(rxBytes: Long, txBytes: Long, nationalBytes: Long, isLte: Boolean)

    suspend fun synchronizeUserDataBytes(data: List<DataBytes>, simId: String)
}