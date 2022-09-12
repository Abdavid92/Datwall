package com.smartsolutions.paquetes.managers.sims

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim

private const val EMBEDDED_SIM_ID = "embedded_sim"

internal class EmbeddedSimManager constructor(
    private val simRepository: ISimRepository
) : InternalSimManager {

    override suspend fun getDefaultSim(type: SimType, relations: Boolean): Result<Sim> {
        return Result.success(embeddedSim(relations))
    }

    override suspend fun isSimDefault(type: SimType, sim: Sim): Boolean {
        return true
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