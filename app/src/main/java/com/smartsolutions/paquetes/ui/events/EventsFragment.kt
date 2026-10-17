package com.smartsolutions.paquetes.ui.events

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abdavid92.persistentlog.Event
import com.abdavid92.persistentlog.EventType
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentEventsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EventsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentEventsBinding? = null

    private val viewModel by viewModels<EventsViewModel> ()

    private val binding get() = _binding!!

    private val adapter = EventsRecyclerAdapter(::showEventDetail)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recycler.adapter = adapter

        binding.spinnerFilterEvents.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                viewModel.onFilterChanged(
                    when (position) {
                        1 -> EventType.Error
                        2 -> EventType.Info
                        3 -> EventType.Warning
                        else -> null
                    }
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.events.collect(::onEvent) }
            }
        }
    }

    private fun render(state: EventsUiState) {
        if (state.loading)
            return

        adapter.submitList(state.events)
        binding.linNoData.visibility = if (state.events.isEmpty()) View.VISIBLE else View.GONE
        binding.progressBar.visibility = View.GONE
    }

    private fun onEvent(event: EventsEvent) {
        when (event) {
            EventsEvent.NoEmailApp ->
                Toast.makeText(requireContext(), R.string.no_email_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEventDetail(event: Event) {
        AlertDialog.Builder(requireContext())
            .setTitle(event.tag)
            .setMessage(event.msg)
            .setPositiveButton(getString(R.string.btn_close), null)
            .setNegativeButton(getString(R.string.send_inform)) { _, _ ->
                viewModel.onSendByEmail(event)
            }.show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = EventsFragment()
    }
}
