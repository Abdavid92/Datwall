package com.smartsolutions.paquetes.ui.packages

import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.data.PackagesRules
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.IDataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PackagesUiState(
    val loading: Boolean = true,
    val availability: PackagesRules.Availability? = null,
    val plans: List<DataPackage> = emptyList(),
    val purchasing: Boolean = false
)

sealed interface PackagesEvent {
    /**
     * La compra se envió: la pantalla espera el SMS de confirmación.
     * */
    data object PurchaseStarted : PackagesEvent

    data object PurchaseFailed : PackagesEvent

    data class RequestPermission(val requestCode: Int) : PackagesEvent
}

/**
 * Planes de una línea. La línea llega en los argumentos del fragmento ([ARG_SIM_ID]); sin ella
 * (pantalla con las pestañas) solo se usan las líneas instaladas.
 * */
@HiltViewModel
class PackagesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager
) : ViewModel() {

    private val _state = MutableStateFlow(PackagesUiState())
    val state: StateFlow<PackagesUiState> = _state.asStateFlow()

    private val _events = Channel<PackagesEvent>(Channel.BUFFERED)
    val events: Flow<PackagesEvent> = _events.receiveAsFlow()

    private var sim: Sim? = null

    init {
        savedStateHandle.get<String>(ARG_SIM_ID)?.let { simId ->
            viewModelScope.launch {
                val sim = simManager.getSim(simId, true)
                this@PackagesViewModel.sim = sim
                _state.value = if (sim == null) {
                    PackagesUiState(loading = false, availability = PackagesRules.Availability.NOT_CONFIGURED)
                } else {
                    PackagesUiState(
                        loading = false,
                        availability = PackagesRules.availability(sim),
                        plans = PackagesRules.visiblePlans(sim)
                    )
                }
            }
        }
    }

    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }

    fun onPurchase(packageId: DataPackages.PackageId) {
        val sim = sim ?: return
        val dataPackage = _state.value.plans.firstOrNull { it.id == packageId } ?: return

        _state.update { it.copy(purchasing = true) }

        viewModelScope.launch {
            try {
                dataPackageManager.buyDataPackage(dataPackage, sim)
                // Se guarda para confirmar la compra con el SMS aunque el proceso muera mientras.
                savedStateHandle[KEY_PURCHASE_SIM_ID] = sim.id
                _events.send(PackagesEvent.PurchaseStarted)
            } catch (e: Exception) {
                when (e) {
                    is USSDRequestException, is MissingPermissionException ->
                        _events.send(PackagesEvent.RequestPermission(IPermissionsManager.CALL_CODE))
                    else -> _events.send(PackagesEvent.PurchaseFailed)
                }
            } finally {
                _state.update { it.copy(purchasing = false) }
            }
        }
    }

    /**
     * El usuario compartió el SMS de confirmación de la última compra.
     * */
    fun onSmsShared(smsBody: String) {
        val simId = savedStateHandle.get<String>(KEY_PURCHASE_SIM_ID) ?: return
        viewModelScope.launch {
            dataPackageManager.confirmPurchaseFromSms(smsBody, simId)
        }
    }

    class HeaderPackagesItem(
        override val id: DataPackages.PackageId = DataPackages.PackageId.DailyBag,
        override val name: String,
        override val description: String = "",
        override val price: Float = 0f,
        override val bytes: Long = 0,
        override val bytesLte: Long = 0,
        override val nationalBytes: Long = 0,
        override val network: String = Networks.NETWORK_NONE,
        override val index: Int = 0,
        override val duration: Int = 0,
        override val smsKey: String = "",
        override var deprecated: Boolean = false,
        override val minutes: Int = 0,
        override val sms: Int = 0
    ) : IDataPackage

    companion object {
        const val ARG_SIM_ID = "sim_id"
        private const val KEY_PURCHASE_SIM_ID = "purchase_sim_id"
    }
}
