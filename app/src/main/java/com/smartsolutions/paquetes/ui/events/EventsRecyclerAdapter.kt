package com.smartsolutions.paquetes.ui.events

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemEventsBinding
import com.smartsolutions.paquetes.repositories.models.Event
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class EventsRecyclerAdapter constructor(
    private var events: List<Event>,
    private val fragment: EventsFragment
) : RecyclerView.Adapter<EventsRecyclerAdapter.ItemViewHolder>(), CoroutineScope {

    private var filter: Event.EventType? = null
    private var filtered = events
    private var eventsShow = events
    private var job: Job? = null
    private var isSearching = false


    fun updateEvents(list: List<Event>) {

        if (isSearching) {
            return
        }

        events = list

        filtered = if (filter == null) {
            events
        } else {
            list.filter { it.type == filter }
        }

        eventsShow = filtered
        if (eventsShow.isEmpty()){
            fragment.setNoData(true)
        }else {
            fragment.setNoData(false)
        }
        notifyDataSetChanged()
    }


    fun setFilter(type: Event.EventType?) {
        filter = type
        updateEvents(events)
    }


    fun search(string: String?) {

        if (job != null) {
            job?.cancel()
            job = null
        }

        job = launch {
            if (string == null) {
                isSearching = false
                updateEvents(events)
            } else {
                isSearching = true
                eventsShow = filtered.filter { it.title.contains(string) }
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }
            }

        }
    }


    inner class ItemViewHolder(private val binding: ItemEventsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {

                typeEvent.text = event.type.name

                linTypeEvent.setBackgroundColor(
                    when (event.type) {
                        Event.EventType.INFO -> Color.BLUE
                        Event.EventType.ERROR -> Color.RED
                        Event.EventType.WARNING -> {
                            binding.typeEvent.setTextColor(Color.BLACK)
                            Color.YELLOW
                        }
                    }
                )

                titleEvent.text = event.title
                descriptionEvent.text = event.message

                dateEvent.text =
                    SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa", Locale.getDefault()).format(
                        Date(event.date)
                    )

                root.setOnClickListener {
                    fragment.showEventDetail(event)
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        return ItemViewHolder(
            ItemEventsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(eventsShow[position])
    }

    override fun getItemCount(): Int {
        return eventsShow.size
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default


}