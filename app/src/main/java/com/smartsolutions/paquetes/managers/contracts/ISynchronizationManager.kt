package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.repositories.models.Sim
import kotlin.jvm.Throws

interface ISynchronizationManager {

    var synchronizationMode: IDataPackageManager.ConnectionMode

    @Throws(Exception::class)
    suspend fun synchronizeUserDataBytes(sim: Sim)

    fun scheduleUserDataBytesSynchronization(intervalInMinutes: Int, sim: Sim? = null)
}