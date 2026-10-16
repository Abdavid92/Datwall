package com.smartsolutions.paquetes.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.BubbleServiceHelper
import com.smartsolutions.paquetes.helpers.FirewallHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val firewallOn: Boolean = false,
    /**
     * Modo dinámico del cortafuegos; false es el modo estático.
     * */
    val dynamicFirewall: Boolean = true,
    val allowedApps: Int = 0,
    val blockedApps: Int = 0,
    val totalApps: Int = 0,
    val bubbleOn: Boolean = false,
    /**
     * Posición del tamaño de la burbuja (0 pequeña, 1 mediana, 2 grande).
     * */
    val bubbleSize: Int = DashboardViewModel.DEFAULT_BUBBLE_SIZE,
    val bubbleTransparency: Float = DashboardViewModel.DEFAULT_BUBBLE_TRANSPARENCY,
    val bubbleAlwaysShow: Boolean = DashboardViewModel.DEFAULT_BUBBLE_ALWAYS_SHOW
)

sealed interface DashboardEvent {
    /**
     * Falta un permiso para el control; el resultado vuelve con [DashboardViewModel.onPermissionGranted]
     * o [DashboardViewModel.onPermissionDenied].
     * */
    data class RequestPermission(val requestCode: Int) : DashboardEvent

    /**
     * No se puede encender el cortafuegos si todas las apps tienen acceso.
     * */
    data object FirewallNeedsBlockedApp : DashboardEvent

    /**
     * El control que tocó el usuario no se aplicó: la pantalla vuelve a dibujar el estado.
     * */
    data object StateOutOfSync : DashboardEvent
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val appsManager: IAppsManager,
    private val permissionsManager: IPermissionsManager,
    private val firewallHelper: FirewallHelper,
    private val bubbleServiceHelper: BubbleServiceHelper,
    private val ussdHelper: USSDHelper,
    private val preferencesRepository: IPreferencesRepository
) : ViewModel() {

    private val apps = MutableStateFlow(Triple(0, 0, 0))

    val state: StateFlow<DashboardUiState> = combine(
        firewallHelper.observeFirewallState(),
        bubbleServiceHelper.observeBubbleChanges(),
        preferencesRepository.preferences,
        apps
    ) { firewallOn, bubbleOn, preferences, apps ->
        DashboardUiState(
            firewallOn = firewallOn,
            dynamicFirewall = preferences.dynamicFirewall ?: true,
            allowedApps = apps.first,
            blockedApps = apps.second,
            totalApps = apps.third,
            bubbleOn = bubbleOn,
            bubbleSize = preferences.bubbleSize
                ?.let { name -> BUBBLE_SIZES.indexOf(name).takeIf { it >= 0 } }
                ?: DEFAULT_BUBBLE_SIZE,
            bubbleTransparency = preferences.bubbleTransparency ?: DEFAULT_BUBBLE_TRANSPARENCY,
            bubbleAlwaysShow = preferences.bubbleAlwaysShow ?: DEFAULT_BUBBLE_ALWAYS_SHOW
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DashboardUiState())

    private val _events = Channel<DashboardEvent>(Channel.BUFFERED)
    val events: Flow<DashboardEvent> = _events.receiveAsFlow()

    init {
        refreshApps()
    }

    /**
     * Cuenta las apps permitidas y bloqueadas; se llama al volver a la pantalla.
     * */
    fun refreshApps() {
        viewModelScope.launch {
            val all = appsManager.all()
            apps.value = Triple(all.count { it.access }, all.count { !it.access }, all.size)
        }
    }

    fun onFirewallToggled(on: Boolean) {
        viewModelScope.launch {
            if (!on) {
                firewallHelper.stopFirewall(true)
            } else if (firewallHelper.allAccess(appsManager.all())) {
                _events.send(DashboardEvent.FirewallNeedsBlockedApp)
            } else if (!firewallHelper.checkFirewallPermission()) {
                _events.send(DashboardEvent.RequestPermission(IPermissionsManager.VPN_CODE))
            } else {
                firewallHelper.startFirewall(true)
            }
        }
    }

    fun onDynamicFirewallToggled(on: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setDynamicFirewall(on)
        }
    }

    fun onBubbleToggled(on: Boolean) {
        viewModelScope.launch {
            if (!on) {
                bubbleServiceHelper.stopBubble(true)
            } else if (!permissionsManager.isGranted(IPermissionsManager.DRAW_OVERLAYS_CODE)) {
                _events.send(DashboardEvent.RequestPermission(IPermissionsManager.DRAW_OVERLAYS_CODE))
            } else {
                bubbleServiceHelper.startBubble(true)
            }
        }
    }

    fun onBubbleSizeChanged(size: Int) {
        viewModelScope.launch {
            preferencesRepository.setBubbleSize(BUBBLE_SIZES[size.coerceIn(BUBBLE_SIZES.indices)])
        }
    }

    fun onBubbleTransparencyChanged(transparency: Float) {
        viewModelScope.launch {
            preferencesRepository.setBubbleTransparency(transparency)
        }
    }

    fun onBubbleAlwaysShowChanged(alwaysShow: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setBubbleAlwaysShow(alwaysShow)
        }
    }

    /**
     * Consulta rápida por USSD (ver [USSD_CREDIT], [USSD_BONUS] y [USSD_MB]).
     * */
    fun onUssdShortcut(code: String) {
        viewModelScope.launch {
            try {
                ussdHelper.sendUSSDRequestLegacy(code, false)
            } catch (e: USSDRequestException) {
                if (e.errorCode == USSDHelper.DENIED_CALL_PERMISSION)
                    _events.send(DashboardEvent.RequestPermission(IPermissionsManager.CALL_CODE))
            }
        }
    }

    /**
     * Se concedió el permiso pedido con [DashboardEvent.RequestPermission].
     * */
    fun onPermissionGranted(requestCode: Int) {
        viewModelScope.launch {
            when (requestCode) {
                IPermissionsManager.VPN_CODE -> firewallHelper.startFirewall(true)
                IPermissionsManager.DRAW_OVERLAYS_CODE -> bubbleServiceHelper.startBubble(true)
            }
        }
    }

    fun onPermissionDenied() {
        _events.trySend(DashboardEvent.StateOutOfSync)
    }

    companion object {
        const val USSD_CREDIT = "*222#"
        const val USSD_BONUS = "*222*266#"
        const val USSD_MB = "*222*328#"

        /**
         * Nombres de los tamaños de la burbuja, en el orden de su control.
         * */
        val BUBBLE_SIZES = listOf("SMALL", "MEDIUM", "LARGE")

        const val DEFAULT_BUBBLE_SIZE = 1
        const val DEFAULT_BUBBLE_TRANSPARENCY = 0.5f
        const val DEFAULT_BUBBLE_ALWAYS_SHOW = true
    }
}
