package com.smartsolutions.paquetes.ui.events

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentEventsBinding
import com.smartsolutions.paquetes.repositories.models.Event
import com.smartsolutions.paquetes.ui.resume.ResumeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EventsFragment : Fragment() {

    private var _binding: FragmentEventsBinding? = null
    private val viewModel by viewModels<EventsViewModel> ()

    private val binding get() = _binding!!
    private var adapter: EventsRecyclerAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonFilter.setOnClickListener {
            showFilterOptions()
        }

        viewModel.getEvents().observe(viewLifecycleOwner){
            setAdapter(it)
        }

    }


    private fun setAdapter(events: List<Event>){
        if (adapter == null){
            adapter = EventsRecyclerAdapter(events, this)
            binding.recycler.adapter = adapter
        }else {
            adapter?.updateEvents(events)
        }
    }

    private fun showFilterOptions(){
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.filter_list_by)
            .setItems(R.array.filter_events) { _, pos ->
               adapter?.setFilter( when(pos){
                   1 -> Event.EventType.ERROR
                   2 -> Event.EventType.INFO
                   3 -> Event.EventType.WARNING
                   else -> null
               })
            }.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    companion object {

        @JvmStatic
        fun newInstance() = EventsFragment()

    }
}