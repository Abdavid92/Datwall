package com.smartsolutions.paquetes.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SimsConfigurationEvent {
    /**
     * Sin el permiso de llamadas no se pueden leer las líneas.
     * */
    data class RequestPermission(val requestCode: Int) : SimsConfigurationEvent

    /**
     * La configuración se dejó para más adelante.
     * */
    data object SetupSkipped : SimsConfigurationEvent

    data object Saved : SimsConfigurationEvent
}

@HiltViewModel
class SimsConfigurationViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val permissionsManager: IPermissionsManager,
    private val simManager: ISimManager,
    private val preferencesRepository: IPreferencesRepository
) : ViewModel() {

    /**
     * Líneas instaladas; null mientras no se han leído.
     * */
    private val _sims = MutableStateFlow<List<Sim>?>(null)
    val sims: StateFlow<List<Sim>?> = _sims.asStateFlow()

    private val _events = Channel<SimsConfigurationEvent>(Channel.BUFFERED)
    val events: Flow<SimsConfigurationEvent> = _events.receiveAsFlow()

    init {
        val permission = permissionsManager.findPermission(IPermissionsManager.CALL_CODE)

        if (permission != null && !permission.checkPermission(permission, context)) {
            _events.trySend(SimsConfigurationEvent.RequestPermission(IPermissionsManager.CALL_CODE))
        } else {
            loadSims()
        }
    }

    fun onPermissionGranted() = loadSims()

    /**
     * Sin permiso se deja la configuración para cuando lo conceda.
     * */
    fun onPermissionDenied() {
        viewModelScope.launch {
            preferencesRepository.setSimSetupSkipped(true)
            _events.send(SimsConfigurationEvent.SetupSkipped)
        }
    }

    fun onSave(defaultDataSim: Sim, defaultVoiceSim: Sim) {
        viewModelScope.launch {
            simManager.setDefaultSimManual(SimType.DATA, defaultDataSim.slotIndex)
            simManager.setDefaultSimManual(SimType.VOICE, defaultVoiceSim.slotIndex)
            _events.send(SimsConfigurationEvent.Saved)
        }
    }

    private fun loadSims() {
        viewModelScope.launch {
            _sims.value = simManager.getInstalledSims()
        }
    }
}
