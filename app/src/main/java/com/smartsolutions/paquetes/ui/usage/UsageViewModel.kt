package com.smartsolutions.paquetes.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.Period
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.UsageAggregator
import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.UsageOrder
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * Porción de la gráfica circular. [label] es null para "otras".
 * */
data class UsageSlice(val bytes: Long, val label: String?, val colour: Int)

/**
 * Consumo de un tipo de tráfico: las apps en el orden elegido, las que pasan del 1 % del total
 * ([main]) y el resto ([others]), que se agrupan en la gráfica.
 * */
data class UsageBreakdown(
    val total: Long,
    val apps: List<UsageApp>,
    val main: List<UsageApp>,
    val others: List<UsageApp>,
    val slices: List<UsageSlice>
)

data class UsageUiState(
    @Period val period: Int = 0,
    val order: UsageOrder = UsageOrder.MAX_USAGE,
    val loading: Boolean = true,
    val breakdowns: Map<TrafficType, UsageBreakdown> = emptyMap()
)

/**
 * Lo comparten la pantalla de consumo y sus pestañas (una por tipo de tráfico).
 * */
@HiltViewModel
class UsageViewModel @Inject constructor(
    private val networkUsageManager: NetworkUsageManager,
    private val simManager: ISimManager,
    private val appsManager: IAppsManager,
    private val dateCalendarUtils: DateCalendarUtils,
    private val preferencesRepository: IPreferencesRepository,
    val iconManager: IIconManager
) : ViewModel() {

    private val _state = MutableStateFlow(
        preferencesRepository.preferences.value.let { UsageUiState(period = it.period(), order = it.order()) }
    )
    val state: StateFlow<UsageUiState> = _state.asStateFlow()

    private val refreshes = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                preferencesRepository.preferences.map { it.period() to it.order() }.distinctUntilChanged(),
                refreshes
            ) { selection, _ -> selection }
                .collectLatest { (period, order) ->
                    _state.update { it.copy(period = period, order = order, loading = true) }
                    val breakdowns = load(period, order)
                    _state.update { it.copy(loading = false, breakdowns = breakdowns) }
                }
        }
    }

    fun onPeriodChanged(@Period period: Int) {
        if (period == _state.value.period)
            return
        viewModelScope.launch {
            preferencesRepository.setUsagePeriod(period)
        }
    }

    fun onFilterChanged(order: UsageOrder) {
        viewModelScope.launch {
            preferencesRepository.setUsageFilter(order.name)
        }
    }

    fun onRefresh() {
        refreshes.update { it + 1 }
    }

    /**
     * Se acaba de conceder el acceso de uso: hasta ahora no había datos.
     * */
    fun onUsageAccessGranted() = onRefresh()

    private suspend fun load(@Period period: Int, order: UsageOrder): Map<TrafficType, UsageBreakdown> {
        val interval = dateCalendarUtils.getTimePeriod(period)
        val traffics = networkUsageManager.getAppsUsage(interval.first, interval.second)
        val apps = appsManager.byUids(traffics.map { it.uid }.toIntArray())

        val simId = simManager.getDefaultSimBoth(SimType.DATA)?.id ?: "unknown"

        return TrafficType.entries.associateWith { type ->
            val (total, sorted) = UsageAggregator.aggregate(apps, traffics, type, order, simId)
            breakdown(total, sorted.map { UsageApp(it, randomColour()) })
        }
    }

    private fun breakdown(total: Long, apps: List<UsageApp>): UsageBreakdown {
        val (main, others) = UsageAggregator.splitByShare(total, apps) { it.app.traffic!!.totalBytes.bytes }

        others.forEach { it.colour = OTHERS_COLOUR }

        val slices = main.map { UsageSlice(it.app.traffic!!.totalBytes.bytes, it.app.name, it.colour) } +
            if (others.isEmpty())
                emptyList()
            else
                listOf(UsageSlice(others.sumOf { it.app.traffic!!.totalBytes.bytes }, null, OTHERS_COLOUR))

        return UsageBreakdown(total, apps, main, others, slices)
    }

    private fun randomColour(): Int =
        (0xFF shl 24) or (Random.nextInt(256) shl 16) or (Random.nextInt(256) shl 8) or Random.nextInt(256)

    private fun AppPreferences.period() = usagePeriod ?: 0

    private fun AppPreferences.order() =
        usageFilter?.let { name -> UsageOrder.entries.firstOrNull { it.name == name } } ?: UsageOrder.MAX_USAGE

    companion object {
        /**
         * Gris de `android.graphics.Color.GRAY`.
         * */
        const val OTHERS_COLOUR = 0xFF888888.toInt()
    }
}
