package com.smartsolutions.paquetes.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.models.ConfigurationStep
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    /**
     * Pasos de configuración pendientes; null mientras se leen.
     * */
    val steps: List<ConfigurationStep>? = null,
    val index: Int = 0
) {

    /**
     * Paso que se muestra; null mientras se leen los pasos o si no hay ninguno.
     * */
    val current: ConfigurationStep? get() = steps?.getOrNull(index)
}

/**
 * Lleva el asistente de configuración inicial. Los pasos pendientes se leen una vez y el índice
 * vive en este ViewModel, que sobrevive al giro de la pantalla. Si el proceso muere se vuelven a
 * calcular los pendientes (los ya completados no salen), así que no hace falta guardar el índice.
 * */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val configurationManager: IConfigurationManager,
    private val kernel: DatwallKernel
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val steps = configurationManager.getUncompletedConfigurations().map { it.step }

            if (steps.isEmpty())
                kernel.restart()
            else
                _state.value = SetupUiState(steps)
        }
    }

    /**
     * El paso actual terminó: se muestra el siguiente o, si era el último, sigue el arranque.
     * */
    fun onStepCompleted() {
        val current = _state.value
        val steps = current.steps ?: return

        if (current.index + 1 >= steps.size)
            kernel.restart()
        else
            _state.value = current.copy(index = current.index + 1)
    }

    /**
     * Se omite la configuración que falta y sigue el arranque.
     * */
    fun onSetupSkipped() {
        kernel.restart()
    }
}
