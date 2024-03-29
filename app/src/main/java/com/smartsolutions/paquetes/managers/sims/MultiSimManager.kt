package com.smartsolutions.paquetes.managers.sims

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim

@RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
internal class MultiSimManager constructor(
    private val context: Context,
    private val subscriptionInfoList: List<SubscriptionInfo>,
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) : InternalSimManager {

    override suspend fun getDefaultSim(type: SimType, relations: Boolean): Result<Sim> {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            simDelegate.getActiveSim(type)?.let { info ->
                getInstalledSims(relations).firstOrNull { it.id == simDelegate.getSimId(info) }
                    ?.let { sim ->

                        return Result.success(sim)
                    }
            }
        }

        return Result.failure(UnsupportedOperationException())
    }

    override suspend fun isSimDefault(type: SimType, sim: Sim): Boolean? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            simDelegate.getActiveSim(type)?.let { info ->
                return sim.id == simDelegate.getSimId(info)
            }
        }

        return null
    }

    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {
        val sims = mutableListOf<Sim>()
        subscriptionInfoList.forEach {
            sims.add(buildSim(it, relations))
        }
        return sims
    }

    private suspend fun buildSim(info: SubscriptionInfo, relations: Boolean): Sim {
        val id = simDelegate.getSimId(info)
        var sim = simRepository.get(id, relations)

        if (sim == null) {

            sim = Sim(id, 0L, Networks.NETWORK_NONE).apply {
                if (info.number != null && info.number.isNotBlank())
                    phone = info.number
            }

            simRepository.create(sim)
        }

        return sim.apply {
            slotIndex = info.simSlotIndex
            icon = info.createIconBitmap(context)
        }
    }
}