package com.smartsolutions.paquetes.managers.sims

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SimManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) : ISimManager {

    override suspend fun getDefaultSimSystem(
        type: SimDelegate.SimType,
        relations: Boolean
    ): Result<Sim> {
        return getSimManager().getDefaultSim(type, relations)
    }


    override suspend fun getDefaultSimManual(type: SimDelegate.SimType, relations: Boolean): Sim? {
        getSimManager().getInstalledSims(relations).let { sims ->
            context.internalDataStore.data.firstOrNull()?.get(
                when(type){
                    SimDelegate.SimType.VOICE -> PreferencesKeys.DEFAULT_VOICE_SLOT
                    SimDelegate.SimType.DATA -> PreferencesKeys.DEFAULT_DATA_SLOT
                }
            )?.let { slot ->
                return sims.firstOrNull { it.slotIndex == slot }
            }
        }
        return null
    }


    override suspend fun setDefaultSimManual(type: SimDelegate.SimType, slot: Int) {

        context.internalDataStore.edit {
            it[
                    when (type) {
                        SimDelegate.SimType.VOICE -> PreferencesKeys.DEFAULT_VOICE_SLOT
                        SimDelegate.SimType.DATA -> PreferencesKeys.DEFAULT_DATA_SLOT
                    }
            ] = slot
        }
    }


    override suspend fun getDefaultSimBoth(type: SimDelegate.SimType, relations: Boolean): Sim? {

        var sim = getDefaultSimSystem(type, relations).getOrNull()

        if (sim == null){
            sim = getDefaultSimManual(type, relations)
        }

        return sim
    }


    override suspend fun isSimDefaultSystem(type: SimDelegate.SimType, sim: Sim): Boolean? {
        return getSimManager().isSimDefault(type, sim)
    }

    override suspend fun isSimDefaultBoth(type: SimDelegate.SimType, sim: Sim): Boolean? {
        var default = getSimManager().isSimDefault(type, sim)

        if (default == null){
            context.internalDataStore.data.firstOrNull()?.get(
                when(type){
                    SimDelegate.SimType.VOICE -> PreferencesKeys.DEFAULT_VOICE_SLOT
                    SimDelegate.SimType.DATA -> PreferencesKeys.DEFAULT_DATA_SLOT
                }
            )?.let { slot ->
                default = sim.slotIndex == slot
            }
        }

        return default
    }


    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {
        return getSimManager().getInstalledSims(relations)
    }


    override fun flowInstalledSims(relations: Boolean): Flow<List<Sim>> {
        return simRepository.flow(relations).map {
            return@map getSimManager().getInstalledSims(relations)
        }
    }


    private fun getSimManager(): InternalSimManager {
        val status = getSimsState()

        return when (status.first) {
            SimsState.Embedded -> {
                EmbeddedSimManager(simRepository)
            }
            SimsState.Single -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && status.second.isNotEmpty()) {
                    SingleSimManager(
                        status.second[0],
                        simDelegate,
                        simRepository
                    )
                } else {
                    EmbeddedSimManager(simRepository)
                }
            }
            SimsState.Multiple -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    MultiSimManager(
                        context,
                        status.second,
                        simDelegate,
                        simRepository
                    )
                } else {
                    EmbeddedSimManager(simRepository)
                }
            }
        }
    }

    private fun getSimsState(): Pair<SimsState, List<SubscriptionInfo>> {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

            val infos = simDelegate.getActiveSimsInfo()

            val status = when (infos.size) {
                0 -> SimsState.Embedded
                1 -> SimsState.Single
                else -> SimsState.Multiple
            }

            return status to infos
        }

        return SimsState.Embedded to emptyList()
    }


    enum class SimsState {
        Embedded,
        Single,
        Multiple
    }
}