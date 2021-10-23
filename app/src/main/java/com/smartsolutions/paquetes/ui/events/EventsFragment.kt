package com.smartsolutions.paquetes.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentEventsBinding
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import com.smartsolutions.paquetes.repositories.models.Event
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class EventsFragment : BottomSheetDialogFragment() {

    @Inject
    lateinit var localFileHelper: LocalFileHelper

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

       binding.spinnerFilterEvents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
           override fun onItemSelected(
               parent: AdapterView<*>?,
               view: View?,
               position: Int,
               id: Long
           ) {
               adapter?.setFilter( when(position){
                   1 -> Event.EventType.ERROR
                   2 -> Event.EventType.INFO
                   3 -> Event.EventType.WARNING
                   else -> null
               })
           }
           override fun onNothingSelected(parent: AdapterView<*>?) {}
       }

        viewModel.getEvents().observe(viewLifecycleOwner){
            setAdapter(it)
            if (it.isEmpty()){
                binding.linNoData.visibility = View.VISIBLE
            }else {
                binding.linNoData.visibility = View.GONE
            }
            binding.progressBar.visibility = View.GONE
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

    fun setNoData(noData: Boolean){
        if (noData){
            binding.linNoData.visibility = View.VISIBLE
        }else {
            binding.linNoData.visibility = View.GONE
        }
    }


    fun showEventDetail(event: Event){
        AlertDialog.Builder(requireContext())
            .setTitle(event.title)
            .setMessage(event.message)
            .setPositiveButton(getString(R.string.btn_close), null)
            .setNegativeButton(getString(R.string.send_inform)) { _, _ ->
                localFileHelper.sendFileByEmail(
                    "smartsolutions.apps.cuba@gmail.com",
                    "Informe de Evento Datwall",
                    event.type.name,
                    "${event.title} \n ${event.message}"
                )
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