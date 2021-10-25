package com.smartsolutions.paquetes.ui.events

import android.app.usage.UsageEvents
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.repositories.IEventRepository
import com.smartsolutions.paquetes.repositories.models.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class EventsViewModel @Inject constructor(
    private val eventRepository: IEventRepository
): ViewModel() {

    var filter: Event.EventType? = null

    fun getEvents(): LiveData<List<Event>>{
        return eventRepository.flow().map { it.sortedByDescending { it.date } }.asLiveData()
    }

}