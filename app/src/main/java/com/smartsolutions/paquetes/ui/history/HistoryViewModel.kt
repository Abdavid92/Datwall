package com.smartsolutions.paquetes.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.HistorySummary
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.IPurchasedPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val loading: Boolean = true,
    val sims: List<Sim> = emptyList(),
    /**
     * Filas de cada línea por su id: por mes, del más reciente al más antiguo, la cabecera del mes
     * ([HistoryViewModel.HeaderPurchasedPackage]) seguida de sus compras. Vacía si la línea no tiene compras.
     * */
    val rows: Map<String, List<IPurchasedPackage>> = emptyMap()
)

/**
 * Lo comparten la pantalla de historial y sus pestañas (una por línea).
 * */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    simManager: ISimManager,
    purchasePackageManager: IPurchasedPackagesManager
) : ViewModel() {

    val state: StateFlow<HistoryUiState> = combine(
        simManager.flowInstalledSims().flowOn(Dispatchers.IO),
        purchasePackageManager.getHistory()
    ) { sims, history ->
        HistoryUiState(
            loading = false,
            sims = sims,
            rows = sims.associate { sim -> sim.id to rows(history, sim.id) }
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HistoryUiState())

    private fun rows(history: List<PurchasedPackage>, simId: String): List<IPurchasedPackage> =
        HistorySummary.months(history, simId).flatMap { month ->
            listOf<IPurchasedPackage>(
                HeaderPurchasedPackage(
                    date = month.date,
                    priceTotal = month.totalPrice,
                    cuantity = month.purchases.size
                )
            ) + month.purchases
        }

    /**
     * Cabecera de un mes: fecha de su compra más reciente, total de precios y número de compras.
     * */
    class HeaderPurchasedPackage(
        override val id: Long = 0,
        override val date: Long = 0,
        override val origin: IDataPackageManager.ConnectionMode = IDataPackageManager.ConnectionMode.USSD,
        override var simId: String = "",
        override var pending: Boolean = false,
        override val dataPackageId: DataPackages.PackageId = DataPackages.PackageId.DailyBag,
        val priceTotal: Int,
        val cuantity: Int
    ) : IPurchasedPackage
}
