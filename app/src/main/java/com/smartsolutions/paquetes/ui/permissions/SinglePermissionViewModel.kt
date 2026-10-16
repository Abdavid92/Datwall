package com.smartsolutions.paquetes.ui.permissions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.PermissionRequestPolicy
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.managers.models.PermissionKind
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

data class SinglePermissionUiState(
    val permission: Permission,
    /**
     * Qué hace el botón "Conceder"; null mientras se calcula.
     * */
    val action: PermissionAction? = null
)

sealed interface SinglePermissionEvent {
    /**
     * Lanzar el diálogo del sistema (o su pantalla de ajustes) para pedir el permiso.
     * */
    data object RequestFromSystem : SinglePermissionEvent

    /**
     * Abrir los ajustes de la app, donde se concede un permiso rechazado de forma permanente.
     * */
    data object OpenAppSettings : SinglePermissionEvent

    /**
     * La hoja terminó: se entrega el resultado y se cierra.
     * */
    data class Finish(val granted: Boolean) : SinglePermissionEvent

    /**
     * Con el permiso de llamadas recién concedido, las líneas todavía están sin configurar: se
     * reinició el arranque y la pantalla abre la pantalla inicial.
     * */
    data object SimSetupPending : SinglePermissionEvent
}

/**
 * Hoja que explica un permiso y lo pide. El código del permiso llega en los argumentos del fragmento.
 *
 * Entrega un único resultado: concedido o no. Si la hoja se descarta sin responder cuenta como "no
 * concedido", pero solo si no se entregó otro antes y nunca por recrearse la pantalla.
 * */
@HiltViewModel
class SinglePermissionViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val permissionsManager: IPermissionsManager,
    private val configurationManager: IConfigurationManager,
    private val kernel: DatwallKernel,
    @ApplicationContext
    private val context: Context
) : ViewModel() {

    private val permission: Permission =
        permissionsManager.findPermission(savedStateHandle[ARG_REQUEST_CODE] ?: -1)
            ?: throw IllegalArgumentException("Permiso desconocido")

    private val _state = MutableStateFlow(SinglePermissionUiState(permission))
    val state: StateFlow<SinglePermissionUiState> = _state.asStateFlow()

    private val _events = Channel<SinglePermissionEvent>(Channel.BUFFERED)
    val events: Flow<SinglePermissionEvent> = _events.receiveAsFlow()

    /**
     * Ya se entregó un resultado. Sobrevive a la recreación para no entregar otro.
     * */
    private var delivered: Boolean
        get() = savedStateHandle[KEY_DELIVERED] ?: false
        set(value) {
            savedStateHandle[KEY_DELIVERED] = value
        }

    /**
     * El usuario fue a los ajustes de la app y falta ver qué hizo al volver.
     * */
    private var waitingAppSettings: Boolean
        get() = savedStateHandle[KEY_WAITING_APP_SETTINGS] ?: false
        set(value) {
            savedStateHandle[KEY_WAITING_APP_SETTINGS] = value
        }

    /**
     * La hoja es visible. [canAskAgain] indica si el sistema todavía deja preguntar por el permiso
     * (lo calcula la pantalla porque necesita la actividad).
     * */
    fun onShown(canAskAgain: Boolean) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                action = PermissionRequestPolicy.decide(
                    granted = isGranted(),
                    requestedBefore = permission.kind == PermissionKind.RUNTIME &&
                        permissionsManager.wasRequested(permission.requestCode),
                    canAskAgain = canAskAgain
                )
            )
        }
    }

    fun onGrantClicked() {
        viewModelScope.launch {
            when (_state.value.action) {
                PermissionAction.Nothing -> deliver(true)
                PermissionAction.Request -> {
                    permissionsManager.markRequested(permission.requestCode)
                    _events.send(SinglePermissionEvent.RequestFromSystem)
                }
                PermissionAction.OpenAppSettings -> {
                    waitingAppSettings = true
                    _events.send(SinglePermissionEvent.OpenAppSettings)
                }
                null -> Unit
            }
        }
    }

    fun onSkipClicked() {
        viewModelScope.launch { deliver(false) }
    }

    /**
     * El diálogo del sistema respondió.
     * */
    fun onRequestPermissionsResult(granted: Boolean) {
        viewModelScope.launch { deliver(granted) }
    }

    /**
     * Se volvió de una pantalla del sistema (acceso de uso, superposición, VPN): se comprueba si el
     * permiso quedó concedido.
     * */
    fun onReturnedFromSystem() {
        viewModelScope.launch { deliver(isGranted()) }
    }

    /**
     * La pantalla volvió a primer plano; si se había ido a los ajustes de la app, se comprueba el
     * permiso.
     * */
    fun onResumed() {
        if (!waitingAppSettings)
            return

        waitingAppSettings = false
        viewModelScope.launch { deliver(isGranted()) }
    }

    /**
     * La hoja se cerró. Devuelve true si hay que entregar "no concedido" porque se descartó sin
     * responder; después de entregarlo (o de que ya se hubiera entregado otro) devuelve false.
     * */
    fun onDismissed(): Boolean {
        if (delivered)
            return false

        delivered = true
        return true
    }

    private fun isGranted() = permission.checkPermission(permission, context)

    private suspend fun deliver(granted: Boolean) {
        if (delivered)
            return

        delivered = true

        // Con el permiso de llamadas la app ve las SIM reales: si todavía no están configuradas, se
        // reinicia el arranque para que se configuren antes de seguir con la acción.
        if (granted && permission.requestCode == IPermissionsManager.CALL_CODE &&
            configurationManager.getUncompletedConfigurations().isNotEmpty()
        ) {
            kernel.restart()
            _events.send(SinglePermissionEvent.SimSetupPending)
        } else {
            _events.send(SinglePermissionEvent.Finish(granted))
        }
    }

    companion object {
        const val ARG_REQUEST_CODE = "request_code"
        private const val KEY_DELIVERED = "delivered"
        private const val KEY_WAITING_APP_SETTINGS = "waiting_app_settings"
    }
}
