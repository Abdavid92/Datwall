package com.smartsolutions.paquetes.managers.sims

import com.smartsolutions.paquetes.repositories.models.Sim

internal interface InternalSimManager {

    suspend fun getDefaultSim(type: SimType, relations: Boolean = false): Result<Sim>

    suspend fun isSimDefault(type: SimType, sim: Sim): Boolean?

    suspend fun getInstalledSims(relations: Boolean = false): List<Sim>
}