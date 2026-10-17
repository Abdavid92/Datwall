package com.smartsolutions.paquetes.ui.permissions

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.PermissionRequestPolicy
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
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

/**
 * Fila de la lista de permisos: el permiso y lo que pasará al pulsar su botón.
 * */
data class PermissionItem(val permission: Permission, val action: PermissionAction)

data class PermissionsUiState(
    val items: List<PermissionItem> = emptyList(),
    val firstUse: Boolean = true
)

sealed interface PermissionsEvent {
    /**
     * Lanzar el diálogo del sistema (o su pantalla de ajustes) para pedir el permiso.
     * */
    data class RequestFromSystem(val permission: Permission) : PermissionsEvent

    /**
     * Abrir los ajustes de la app, donde se concede un permiso rechazado de forma permanente.
     * */
    data object OpenAppSettings : PermissionsEvent

    /**
     * Con el permiso de llamadas recién concedido, las líneas todavía están sin configurar: se
     * reinició el arranque y la pantalla abre la pantalla inicial.
     * */
    data object SimSetupPending : PermissionsEvent
}

/**
 * Explicación del uso de datos y lista de permisos. En el primer uso muestra solo los permisos que
 * se piden al inicio; desde Ajustes, todos.
 *
 * Lo que depende de la actividad (qué permisos el sistema todavía deja preguntar) llega como un
 * conjunto de claves.
 * */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val permissionsManager: IPermissionsManager,
    private val configurationManager: IConfigurationManager,
    private val preferences: IPreferencesRepository,
    private val kernel: DatwallKernel,
    @ApplicationContext
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(PermissionsUiState())
    val state: StateFlow<PermissionsUiState> = _state.asStateFlow()

    private val _events = Channel<PermissionsEvent>(Channel.BUFFERED)
    val events: Flow<PermissionsEvent> = _events.receiveAsFlow()

    /**
     * Claves de todos los permisos del sistema; la pantalla averigua cuáles admiten rationale.
     * */
    val permissionKeys: Set<String> =
        permissionsManager.permissions.flatMap { it.keys.asList() }.toSet()

    /**
     * La pantalla es visible: se calcula la acción de cada fila. [rationaleKeys] son las claves
     * para las que el sistema todavía deja volver a preguntar.
     * */
    fun onShown(firstUse: Boolean, rationaleKeys: Set<String>) {
        viewModelScope.launch { refresh(firstUse, rationaleKeys) }
    }

    fun onPermissionClicked(item: PermissionItem) {
        viewModelScope.launch {
            when (item.action) {
                PermissionAction.Nothing -> Unit
                PermissionAction.OpenAppSettings -> _events.send(PermissionsEvent.OpenAppSettings)
                PermissionAction.Request -> {
                    permissionsManager.markRequested(item.permission.requestCode)
                    _events.send(PermissionsEvent.RequestFromSystem(item.permission))
                }
            }
        }
    }

    /**
     * El diálogo del sistema respondió.
     * */
    fun onSystemResult(requestCode: Int, granted: Boolean, rationaleKeys: Set<String>) {
        viewModelScope.launch {
            val firstUse = _state.value.firstUse

            // Con el permiso de llamadas la app ve las SIM reales: si todavía no están configuradas,
            // se reinicia el arranque para que se configuren.
            if (!firstUse && granted && requestCode == IPermissionsManager.CALL_CODE &&
                configurationManager.getUncompletedConfigurations().isNotEmpty()
            ) {
                kernel.restart()
                _events.send(PermissionsEvent.SimSetupPending)
            } else {
                refresh(firstUse, rationaleKeys)
            }
        }
    }

    /**
     * Cierra el primer uso (el usuario continuó o eligió "Ahora no") y sigue con el arranque.
     * */
    fun onFinishFirstUse() {
        viewModelScope.launch {
            preferences.setDataDisclosureAnswered(true)
            kernel.restart()
        }
    }

    private suspend fun refresh(firstUse: Boolean, rationaleKeys: Set<String>) {
        val permissions = if (firstUse)
            permissionsManager.permissions.filter { it.atStartup }
        else
            permissionsManager.permissions

        _state.value = PermissionsUiState(
            items = permissions.map {
                PermissionItem(it, actionOf(it, rationaleKeys))
            },
            firstUse = firstUse
        )
    }

    private suspend fun actionOf(permission: Permission, rationaleKeys: Set<String>) =
        PermissionRequestPolicy.decide(
            granted = permission.checkPermission(permission, context),
            requestedBefore = permission.keys.isNotEmpty() &&
                permissionsManager.wasRequested(permission.requestCode),
            canAskAgain = permission.keys.any { it in rationaleKeys }
        )
}
