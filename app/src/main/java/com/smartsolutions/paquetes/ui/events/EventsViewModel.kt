package com.smartsolutions.paquetes.ui.events

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.abdavid92.persistentlog.Event
import com.abdavid92.persistentlog.EventType
import com.abdavid92.persistentlog.LogManager
import kotlinx.coroutines.flow.map

class EventsViewModel : ViewModel() {

    var filter: EventType? = null

    fun getEvents(): LiveData<List<Event>> {

        val manager = LogManager.newInstance()

        return manager.flow()
            .map { it.sortedByDescending { s -> s.date } }
            .asLiveData()
    }

}