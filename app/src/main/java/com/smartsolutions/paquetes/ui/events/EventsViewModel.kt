package com.smartsolutions.paquetes.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abdavid92.persistentlog.Event
import com.abdavid92.persistentlog.EventType
import com.abdavid92.persistentlog.LogManager
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class EventsUiState(
    val loading: Boolean = true,
    /**
     * Eventos que se muestran: del más reciente al más antiguo y solo del tipo de [filter].
     * */
    val events: List<Event> = emptyList(),
    /**
     * Tipo elegido; null muestra todos.
     * */
    val filter: EventType? = null
)

sealed interface EventsEvent {
    /**
     * No hay una app de correo para enviar el informe.
     * */
    data object NoEmailApp : EventsEvent
}

@HiltViewModel
class EventsViewModel @Inject constructor(
    logManager: LogManager,
    private val localFileHelper: LocalFileHelper
) : ViewModel() {

    private val filter = MutableStateFlow<EventType?>(null)

    val state: StateFlow<EventsUiState> = combine(
        logManager.flow()
            .map { events -> events.sortedByDescending { it.date } }
            .flowOn(Dispatchers.Default),
        filter
    ) { events, filter ->
        EventsUiState(
            loading = false,
            events = if (filter == null) events else events.filter { it.type == filter },
            filter = filter
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, EventsUiState())

    private val _events = Channel<EventsEvent>(Channel.BUFFERED)
    val events: Flow<EventsEvent> = _events.receiveAsFlow()

    fun onFilterChanged(type: EventType?) {
        filter.value = type
    }

    /**
     * Envía el evento por correo al equipo de desarrollo.
     * */
    fun onSendByEmail(event: Event) {
        val sent = localFileHelper.sendFileByEmail(
            BuildConfig.DEVELOPERS_EMAIL,
            "Informe de Evento Datwall",
            event.type.name,
            "${event.tag} \n ${event.msg}"
        )

        if (!sent)
            _events.trySend(EventsEvent.NoEmailApp)
    }
}
