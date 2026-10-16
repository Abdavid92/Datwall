package com.smartsolutions.paquetes.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.UsageChartGrouping
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Consumo de una app en el periodo elegido en la pantalla de consumo.
 * */
class UsageDetails(
    val traffics: List<Traffic>,
    val timeUnit: DateCalendarUtils.MyTimeUnit
)

@HiltViewModel
class UsageAppDetailsViewModel @Inject constructor(
    private val dateCalendarUtils: DateCalendarUtils,
    private val networkUsageManager: NetworkUsageManager,
    private val preferencesRepository: IPreferencesRepository
): ViewModel() {

    /**
     * Null mientras se carga.
     * */
    private val _details = MutableStateFlow<UsageDetails?>(null)
    val details: StateFlow<UsageDetails?> = _details.asStateFlow()

    private var loadedUid: Int? = null

    fun load(uid: Int) {
        if (loadedUid == uid)
            return

        loadedUid = uid

        viewModelScope.launch {
            // Si el usuario nunca cambió el periodo, no hay valor guardado: se usa "Hoy", como la
            // pantalla de consumo.
            val period = preferencesRepository.awaitLoaded().usagePeriod ?: DateCalendarUtils.PERIOD_TODAY

            val interval = dateCalendarUtils.getTimePeriod(period)
            val timeUnit = UsageChartGrouping.appTimeUnit(period)

            _details.value = UsageDetails(
                networkUsageManager.getAppUsageByLapsusTime(uid, interval.first, interval.second, timeUnit),
                timeUnit
            )
        }
    }
}
