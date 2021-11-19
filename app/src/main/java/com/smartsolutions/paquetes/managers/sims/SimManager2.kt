package com.smartsolutions.paquetes.managers.sims

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.repositories.SimRepository
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SimManager2 @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) {

    suspend fun forceModeSingleSim(force: Boolean) {
        context.internalDataStore.edit {
            it[PreferencesKeys.FORCE_MODE_SINGLE_SIM] = force
        }
    }


    suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean = false): Result<Sim> {
        getSimManager()?.let {
            return it.getDefaultSim(type, relations)
        }
        return Result.Failure(NoSuchElementException())
    }

    suspend fun getInstalledSims(relations: Boolean = false): List<Sim> {
        getSimManager()?.let {
            return it.getInstalledSims(relations)
        }
        return emptyList()
    }

    fun flowInstalledSims(relations: Boolean = false): Flow<List<Sim>> {
        return simRepository.flow(relations).map {
            getSimManager()?.let {
                return@map it.getInstalledSims(relations)
            }
            return@map emptyList<Sim>()
        }
    }


    private suspend fun getSimManager(): InternalSimManager? {
        val status = getSimsState()
        return when (status.first) {
            SimsState.None -> {
                if (context.internalDataStore.data.firstOrNull()
                        ?.get(PreferencesKeys.FORCE_MODE_SINGLE_SIM) == true
                ) {
                    EmbeddedSimManager(simRepository)
                } else {
                    null
                }
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
                }else {
                    EmbeddedSimManager(simRepository)
                }
            }
        }
    }

    private fun getSimsState(): Pair<SimsState, List<SubscriptionInfo>> {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

            val infos = simDelegate.getActiveSimsInfo()
            val status = when (infos.size) {
                0 -> SimsState.None
                1 -> SimsState.Single
                else -> SimsState.Multiple
            }

            return status to infos
        }

        return SimsState.Single to emptyList()
    }


    enum class SimsState {
        None,
        Single,
        Multiple
    }
}