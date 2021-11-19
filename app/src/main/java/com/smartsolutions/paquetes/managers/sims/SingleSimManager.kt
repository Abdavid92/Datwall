package com.smartsolutions.paquetes.managers.sims

import android.os.Build
import android.telephony.SubscriptionInfo
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
internal class SingleSimManager constructor(
    private val simDelegate: SimDelegate,
    private val subscriptionInfo: SubscriptionInfo,
    private val simRepository: ISimRepository
) : InternalSimManager {

    override suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean): Result<Sim> {

        return Result.Success(getSingleSim(relations))
    }

    override suspend fun setDefaultSim(type: SimDelegate.SimType, sim: Sim): Boolean {
        return true
    }

    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {

        return listOf(getSingleSim(relations))
    }

    private suspend fun getSingleSim(relations: Boolean): Sim {

        val id = simDelegate.getSimId(subscriptionInfo)

        var sim = simRepository.get(id, relations)

        if (sim == null) {

            sim = Sim(id, 0L, Networks.NETWORK_NONE).apply {
                defaultData = true
                defaultVoice = true

                if (subscriptionInfo.number != null && subscriptionInfo.number.isNotBlank())
                    phone = subscriptionInfo.number
            }

            simRepository.create(sim)
        }

        return sim.apply {
            slotIndex = subscriptionInfo.simSlotIndex
        }
    }
}