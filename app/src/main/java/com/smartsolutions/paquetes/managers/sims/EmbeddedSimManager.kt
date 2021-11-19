package com.smartsolutions.paquetes.managers.sims

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result

private const val EMBEDDED_SIM_ID = "embedded_sim"

internal class EmbeddedSimManager constructor(
    private val simRepository: ISimRepository
): InternalSimManager {

    override suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean): Result<Sim> {
        return Result.Success(embeddedSim(relations))
    }

    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {
        return listOf(embeddedSim(relations))
    }

    private suspend fun embeddedSim(relations: Boolean): Sim {
        var sim = simRepository.get(EMBEDDED_SIM_ID, relations)

        if (sim == null) {
            sim = Sim(EMBEDDED_SIM_ID, 0L, Networks.NETWORK_NONE)
            simRepository.create(sim)
        }

        return sim.apply {
            this.slotIndex = 0
        }
    }
}