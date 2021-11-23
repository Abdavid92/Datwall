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
    private val subscriptionInfo: SubscriptionInfo,
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) : InternalSimManager {

    override suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean): Result<Sim> {
        return Result.Success(singleSim(relations))
    }

    override suspend fun isSimDefault(type: SimDelegate.SimType, sim: Sim): Boolean? {
        val result = getDefaultSim(type, false)
        if (result.isSuccess){
            return (result as Result.Success).value.id == sim.id
        }
        return null
    }

    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {
        return listOf(singleSim(relations))
    }

    private suspend fun singleSim(relations: Boolean): Sim {
        val id = simDelegate.getSimId(subscriptionInfo)
        var sim = simRepository.get(id, relations)

        if (sim == null) {

            sim = Sim(id, 0L, Networks.NETWORK_NONE).apply {
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