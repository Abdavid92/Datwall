package com.smartsolutions.paquetes.managers.sims

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SimManager2 @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val simDelegate: SimDelegate
) {

    fun getSimsState(): SimsState {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {

            return when (simDelegate.getActiveSimsInfo().size) {
                0 -> SimsState.None
                1 -> SimsState.Single
                else -> SimsState.Multiple
            }
        }

        return SimsState.Single
    }

    suspend fun forceModeSingleSim(force: Boolean) {
        context.internalDataStore.edit {
            it[PreferencesKeys.FORCE_MODE_SINGLE_SIM] = force
        }
    }

    suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean = false): Result<Sim> {
        TODO("Not yet implemented")
    }

    suspend fun setDefaultSim(type: SimDelegate.SimType, sim: Sim): Boolean {
        TODO("Not yet implemented")
    }

    suspend fun getInstalledSims(relations: Boolean = false): List<Sim> {
        TODO("Not yet implemented")
    }

    fun flowInstalledSims(relations: Boolean = false): Flow<List<Sim>> {
        TODO("Not yet implemented")
    }

    enum class SimsState {
        None,
        Single,
        Multiple
    }
}