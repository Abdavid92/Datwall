package com.smartsolutions.paquetes.managers.sims

import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result

internal interface InternalSimManager {

    suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean = false): Result<Sim>

    suspend fun getInstalledSims(relations: Boolean = false): List<Sim>
}