package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.repositories.models.DataPackage

interface IUserDataBytesManager {

    suspend fun addDataBytes(dataPackage: DataPackage, simIndex: Int)
}