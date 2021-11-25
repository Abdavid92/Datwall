package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import kotlinx.coroutines.flow.Flow

interface ISimManager2 {
    suspend fun forceModeSingleSim(force: Boolean)

    suspend fun getDefaultSimSystem(type: SimDelegate.SimType, relations: Boolean = false): Result<Sim>

    suspend fun getDefaultSimManual(type: SimDelegate.SimType, relations: Boolean = false): Sim?

    suspend fun setDefaultSimManual(type: SimDelegate.SimType, slot: Int)

    suspend fun isSimDefaultSystem(type: SimDelegate.SimType, sim: Sim): Boolean?

    suspend fun getInstalledSims(relations: Boolean = false): List<Sim>
    fun flowInstalledSims(relations: Boolean = false): Flow<List<Sim>>

    enum class SimsState {
        None,
        Single,
        Multiple
    }
}