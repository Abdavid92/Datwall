package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.UserDataSummary
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IStatisticsManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.UserDataOrder
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.apache.commons.lang3.time.DateUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Datos de una línea: los vigentes en el orden elegido, los totales y las medias.
 * */
data class ResumeLineState(
    val data: List<UserDataBytes>,
    val summary: UserDataSummary,
    /**
     * Consumo medio diario de la última semana.
     * */
    val dailyAverage: DataUnitBytes? = null,
    /**
     * Lo que se puede gastar cada día hasta que venzan los datos.
     * */
    val dailyRemainder: DataUnitBytes? = null
)

data class ResumeUiState(
    val sims: List<Sim> = emptyList(),
    val lines: Map<String, ResumeLineState> = emptyMap(),
    val order: UserDataOrder = UserDataOrder.NORMAL,
    val synchronizing: Boolean = false,
    /**
     * Cambia con cada actualización. `UserDataBytes.equals` solo compara tipo y línea, así que
     * sin esto un cambio de megas parecería "el mismo estado" y no llegaría a la pantalla.
     * */
    val revision: Long = 0
)

/**
 * Datos de una línea tal como llegaron de la base de datos. Se compara por identidad a propósito:
 * cada lectura es distinta aunque `UserDataBytes.equals` diga lo contrario.
 * */
private class LineData(val data: List<UserDataBytes>)

sealed interface ResumeEvent {
    data class RequestPermission(val requestCode: Int) : ResumeEvent

    data object RequestAccessibility : ResumeEvent

    /**
     * La sincronización falló; [ussdErrorCode] es null si no fue un error de USSD.
     * */
    data class SyncFailed(val ussdErrorCode: Int?) : ResumeEvent
}

/**
 * Lo comparten la pantalla de resumen y sus pestañas (una por línea).
 * */
@HiltViewModel
class ResumeViewModel @Inject constructor(
    simManager: ISimManager,
    private val userDataManager: IUserDataManager,
    private val synchronizationManager: ISynchronizationManager,
    private val statisticsManager: IStatisticsManager,
    private val preferencesRepository: IPreferencesRepository
) : ViewModel() {

    private val userData = MutableStateFlow<Map<String, LineData>>(emptyMap())
    private var revision = 0L
    private val averages = MutableStateFlow<Map<String, Pair<DataUnitBytes, DataUnitBytes>>>(emptyMap())
    private val synchronizing = MutableStateFlow(false)

    private val order = preferencesRepository.preferences
        .map { prefs ->
            prefs.resumeFilter
                ?.let { name -> UserDataOrder.entries.firstOrNull { it.name == name } }
                ?: UserDataOrder.NORMAL
        }
        .distinctUntilChanged()

    val state: StateFlow<ResumeUiState> = combine(
        simManager.flowInstalledSims().flowOn(Dispatchers.IO),
        userData,
        averages,
        order,
        synchronizing
    ) { sims, userData, averages, order, synchronizing ->
        ResumeUiState(
            sims = sims,
            lines = userData.mapValues { (simId, line) ->
                val data = line.data
                ResumeLineState(
                    data = UserDataSummary.visible(data, order),
                    summary = UserDataSummary.of(data),
                    dailyAverage = averages[simId]?.first,
                    dailyRemainder = averages[simId]?.second
                )
            },
            order = order,
            synchronizing = synchronizing,
            revision = ++revision
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ResumeUiState())

    private val _events = Channel<ResumeEvent>(Channel.BUFFERED)
    val events: Flow<ResumeEvent> = _events.receiveAsFlow()

    private val watchedLines = mutableSetOf<String>()

    /**
     * Línea que se sincronizó por última vez, para reintentar al conceder el permiso.
     * */
    private var lastSyncSimId: String? = null

    /**
     * Empieza a seguir los datos de [simId] (la pestaña de esa línea está visible).
     * */
    fun onLineShown(simId: String) {
        if (!watchedLines.add(simId))
            return

        viewModelScope.launch {
            userDataManager.flowUserData(simId).collect { data ->
                userData.update { it + (simId to LineData(data)) }

                val now = System.currentTimeMillis()
                val usage = statisticsManager.getAverage(now - DateUtils.MILLIS_PER_DAY * 7, now, TimeUnit.DAYS)
                val rest = statisticsManager.getRemainder(TimeUnit.DAYS, data)
                averages.update { it + (simId to (usage to rest)) }
            }
        }
    }

    fun onFilterChanged(order: UserDataOrder) {
        viewModelScope.launch {
            preferencesRepository.setResumeFilter(order.name)
        }
    }

    fun onSyncClicked(simId: String) {
        val sim = state.value.sims.firstOrNull { it.id == simId } ?: return
        if (synchronizing.value)
            return

        lastSyncSimId = simId
        synchronizing.value = true

        viewModelScope.launch {
            try {
                synchronizationManager.synchronizeUserDataBytes(sim)
            } catch (e: USSDRequestException) {
                _events.send(
                    when (e.errorCode) {
                        USSDHelper.ACCESSIBILITY_SERVICE_UNAVAILABLE -> ResumeEvent.RequestAccessibility
                        USSDHelper.DENIED_CALL_PERMISSION -> ResumeEvent.RequestPermission(IPermissionsManager.CALL_CODE)
                        else -> ResumeEvent.SyncFailed(e.errorCode)
                    }
                )
            } catch (e: Exception) {
                _events.send(ResumeEvent.SyncFailed(null))
            } finally {
                synchronizing.value = false
            }
        }
    }

    /**
     * Se concedió el permiso (o se activó la accesibilidad) que pidió la sincronización:
     * se continúa la que pidió el usuario.
     * */
    fun onPermissionGranted() {
        lastSyncSimId?.let { onSyncClicked(it) }
    }
}
