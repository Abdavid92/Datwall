package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.repositories.models.DataBytes

interface IUserDataBytesManager {

    suspend fun registerTraffic(rxBytes: Long, txBytes: Long, nationalBytes: Long, messagingBytes: Long, isLte: Boolean)

    suspend fun synchronizeUserDataBytes(data: List<DataBytes>, simId: String)
}