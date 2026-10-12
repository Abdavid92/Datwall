package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.DataPackage

interface IUserDataBytesManager {

    suspend fun addDataBytes(dataPackage: DataPackage, simId: String)

    suspend fun addPromoBonus(simId: String, bytes: Long, bytesLte: Long)

    suspend fun addMessagingBag(simId: String, dataPackage: DataPackage)

    suspend fun registerTraffic(rxBytes: Long, txBytes: Long, nationalBytes: Long, messagingBytes: Long, isLte: Boolean)

    suspend fun synchronizeUserDataBytes(data: List<DataBytes>, simId: String)
}