package com.smartsolutions.paquetes.ui.settings.sim

import android.app.Application
import android.os.Build
import androidx.lifecycle.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.SimManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DefaultSimsViewModel @Inject constructor(
    private val simManager: ISimManager,
    application: Application
) : AndroidViewModel(application) {


    fun getInstalledSims(): LiveData<Pair<List<Sim>, Boolean>> {
        return simManager.flowInstalledSims().map {
            return@map it to isSystemDualSimBroken()
        }.asLiveData(Dispatchers.Default)
    }

    fun setDefaultSim(sim: Sim, simType: SimDelegate.SimType) {
        viewModelScope.launch(Dispatchers.IO) {
            simManager.setDefaultSim(simType, sim)
        }
    }

    private suspend fun isSystemDualSimBroken(): Boolean {
        if (simManager.isSeveralSimsInstalled()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
                Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ||
                getApplication<Application>().internalDataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.IS_DUAL_SIM_BROKEN) == true
            ) {
                return true
            }
        }
        return false
    }

}