package com.smartsolutions.paquetes.ui.events

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.abdavid92.persistentlog.Event
import com.abdavid92.persistentlog.EventType
import com.smartsolutions.paquetes.databinding.ItemEventsBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class EventsRecyclerAdapter constructor(
    private var events: List<Event>,
    private val fragment: EventsFragment
) : RecyclerView.Adapter<EventsRecyclerAdapter.ItemViewHolder>(), CoroutineScope {

    private var filter: EventType? = null
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


    fun setFilter(type: EventType?) {
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
                eventsShow = filtered.filter { it.tag?.contains(string) == true }
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }
            }

        }
    }


    inner class ItemViewHolder(var binding: ItemEventsBinding?) :
        RecyclerView.ViewHolder(binding!!.root) {

        fun bind(event: Event) {
            binding?.apply {

                typeEvent.text = event.type.name

                linTypeEvent.setBackgroundColor(
                    when (event.type) {
                        EventType.Info -> Color.BLUE
                        EventType.Error -> Color.RED
                        EventType.Warning -> {
                           typeEvent.setTextColor(Color.BLACK)
                            Color.YELLOW
                        }
                        EventType.Debug -> Color.GREEN
                    }
                )

                titleEvent.text = event.tag
                descriptionEvent.text = event.msg

                dateEvent.text =
                    SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa", Locale.getDefault()).format(
                        event.date
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