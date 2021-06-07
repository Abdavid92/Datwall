package com.smartsolutions.paquetes.managers

import android.os.Build
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_NONE
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import kotlin.coroutines.suspendCoroutine

class SimManager @Inject constructor(
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) {

    private val embeddedSimId = "legacy_sim"

    suspend fun getDefaultVoiceSim(): Sim {
        TODO()
    }

    suspend fun getDefaultDataSim(): Sim {
        TODO()
    }

    suspend fun isInstalledSeveralSims(): Boolean =
        getInstalledSims().size > 1

    suspend fun getInstalledSims(): List<Sim> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return listOf(seedEmbeddedSim())
        }
        TODO()
    }

    private suspend fun seedEmbeddedSim(): Sim {
        simRepository.get(embeddedSimId)?.let {
            return it
        }

        val sim = Sim(embeddedSimId, 0, NETWORK_NONE)

        simRepository.create(sim)

        return sim
    }
}