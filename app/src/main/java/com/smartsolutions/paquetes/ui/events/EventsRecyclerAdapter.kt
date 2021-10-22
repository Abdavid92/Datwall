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
) : RecyclerView.Adapter<EventsRecyclerAdapter.ItemViewHolder>(), CoroutineScope{

    private var filter: Event.EventType? = null
    private var filtered = events
    private var eventsShow = events
    private var job: Job? = null


    fun updateEvents(list: List<Event>){
        events = list

        if (job != null){
            job?.cancel()
            job = null
        }

        job = launch {
            filtered = if (filter == null){
                events
            }else {
                list.filter { it.type == filter }
            }

            val result = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return eventsShow.size
                }

                override fun getNewListSize(): Int {
                    return filtered.size
                }

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return filtered[newItemPosition] == eventsShow[oldItemPosition]
                }

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                    return filtered[newItemPosition].date == eventsShow[oldItemPosition].date
                }

            })

            eventsShow = filtered

            withContext(Dispatchers.Main) {
                result.dispatchUpdatesTo(this@EventsRecyclerAdapter)
            }
        }
    }


    fun setFilter(type: Event.EventType?){
        filter = type
        updateEvents(events)
    }



    inner class ItemViewHolder(private val binding: ItemEventsBinding): RecyclerView.ViewHolder(binding.root){

        fun bind(event: Event){
            binding.apply {

                typeEvent.text = event.type.name

                linTypeEvent.setBackgroundColor(
                    when(event.type){
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

                dateEvent.text = SimpleDateFormat("dd-MMM-yyyy hh:mm aa", Locale.getDefault()).format(
                    Date(event.date)
                )
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