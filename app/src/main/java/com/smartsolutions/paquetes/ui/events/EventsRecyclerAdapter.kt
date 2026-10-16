package com.smartsolutions.paquetes.ui.events

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.abdavid92.persistentlog.Event
import com.abdavid92.persistentlog.EventType
import com.smartsolutions.paquetes.databinding.ItemEventsBinding
import java.text.SimpleDateFormat
import java.util.*

class EventsRecyclerAdapter(
    private val onEventClicked: (Event) -> Unit
) : ListAdapter<Event, EventsRecyclerAdapter.ItemViewHolder>(DiffCallback) {

    inner class ItemViewHolder(var binding: ItemEventsBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: Event) {
            binding.apply {
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
                    onEventClicked(event)
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
        holder.bind(getItem(position))
    }

    private object DiffCallback : DiffUtil.ItemCallback<Event>() {

        override fun areItemsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem

        override fun areContentsTheSame(oldItem: Event, newItem: Event) = oldItem == newItem
    }
}
