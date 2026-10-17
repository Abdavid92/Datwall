package com.smartsolutions.paquetes.ui.permissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.helpers.AccessibilityServiceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

sealed interface StartAccessibilityEvent {
    /**
     * La hoja terminó: se entrega el resultado y se cierra.
     * */
    data class Finish(val granted: Boolean) : StartAccessibilityEvent
}

/**
 * Hoja que lleva al usuario a activar el servicio de accesibilidad. Entrega un único resultado:
 * activado o no. Si se descarta sin responder cuenta como "no activado", pero solo si no se
 * entregó otro antes y nunca por recrearse la pantalla.
 * */
@HiltViewModel
class StartAccessibilityServiceViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val accessibilityServiceHelper: AccessibilityServiceHelper
) : ViewModel() {

    private val _events = Channel<StartAccessibilityEvent>(Channel.BUFFERED)
    val events: Flow<StartAccessibilityEvent> = _events.receiveAsFlow()

    private var delivered: Boolean
        get() = savedStateHandle[KEY_DELIVERED] ?: false
        set(value) {
            savedStateHandle[KEY_DELIVERED] = value
        }

    /**
     * El usuario fue a los ajustes del sistema y falta ver qué hizo al volver.
     * */
    private var waitingSettings: Boolean
        get() = savedStateHandle[KEY_WAITING_SETTINGS] ?: false
        set(value) {
            savedStateHandle[KEY_WAITING_SETTINGS] = value
        }

    fun onOpenSettingsClicked() {
        accessibilityServiceHelper.openAccessibilityServicesActivity()
        waitingSettings = true
    }

    fun onSkipClicked() = deliver(false)

    /**
     * La pantalla volvió a primer plano; si venía de los ajustes se comprueba el servicio.
     * */
    fun onResumed() {
        if (!waitingSettings)
            return

        waitingSettings = false
        deliver(accessibilityServiceHelper.accessibilityServiceEnabled())
    }

    /**
     * La hoja se cerró. Devuelve true si hay que entregar "no activado" porque se descartó sin
     * responder; después de entregarlo (o de que ya se hubiera entregado otro) devuelve false.
     * */
    fun onDismissed(): Boolean {
        if (delivered)
            return false

        delivered = true
        return true
    }

    private fun deliver(granted: Boolean) {
        if (delivered)
            return

        delivered = true
        _events.trySend(StartAccessibilityEvent.Finish(granted))
    }

    private companion object {
        const val KEY_DELIVERED = "delivered"
        const val KEY_WAITING_SETTINGS = "waiting_settings"
    }
}
