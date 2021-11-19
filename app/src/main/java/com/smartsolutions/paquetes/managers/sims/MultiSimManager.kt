package com.smartsolutions.paquetes.managers.sims

import android.telephony.SubscriptionInfo
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result

internal class MultiSimManager constructor(
    private val simDelegate: SimDelegate,
    private val subscriptionInfoList: List<SubscriptionInfo>,
    private val simRepository: ISimRepository
): InternalSimManager {

    override suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean): Result<Sim> {
        TODO("Not yet implemented")
    }

    override suspend fun setDefaultSim(type: SimDelegate.SimType, sim: Sim): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {

        

        TODO("Not yet implemented")
    }
}