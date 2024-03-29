package com.smartsolutions.paquetes.ui.settings

import android.app.Application
import androidx.lifecycle.*
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SimsConfigurationViewModel @Inject constructor(
    application: Application,
    permissionsManager: IPermissionsManager,
    private val simManager: ISimManager
): AndroidViewModel(application) {

    private val delegate = SimsDelegate(
        getApplication(),
        simManager,
        permissionsManager,
        viewModelScope
    )

    fun getSims(
        fragment: AbstractSettingsFragment
    ): LiveData<List<Sim>> {
        return delegate.getSims(fragment)
    }

    fun saveChanges(defaultDataSim: Sim, defaultVoiceSim: Sim, onComplete: () -> Unit) {
        viewModelScope.launch {

            simManager.setDefaultSimManual(SimType.DATA, defaultDataSim.slotIndex)
            simManager.setDefaultSimManual(SimType.VOICE, defaultVoiceSim.slotIndex)

            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }
}