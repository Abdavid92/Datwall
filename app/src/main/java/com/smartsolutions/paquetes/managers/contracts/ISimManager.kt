package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import kotlinx.coroutines.flow.Flow

interface ISimManager {

    suspend fun getDefaultSimSystem(type: SimType, relations: Boolean = false): Result<Sim>

    suspend fun getDefaultSimManual(type: SimType, relations: Boolean = false): Sim?

    suspend fun setDefaultSimManual(type: SimType, slot: Int)

    suspend fun getDefaultSimBoth(type: SimType, relations: Boolean = false): Sim?

    suspend fun isSimDefaultSystem(type: SimType, sim: Sim): Boolean?

    suspend fun isSimDefaultBoth(type: SimType, sim: Sim): Boolean?

    suspend fun getInstalledSims(relations: Boolean = false): List<Sim>

    fun flowInstalledSims(relations: Boolean = false): Flow<List<Sim>>
}