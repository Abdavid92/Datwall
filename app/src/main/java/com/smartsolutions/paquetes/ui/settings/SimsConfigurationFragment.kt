package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.onPermissionResult
import com.smartsolutions.paquetes.ui.permissions.showPermissionSheet
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SimsConfigurationFragment : AbstractSettingsFragment(R.layout.fragment_sims_configuration) {

    private val viewModel by viewModels<SimsConfigurationViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        onPermissionResult { _, granted ->
            if (granted)
                viewModel.onPermissionGranted()
            else
                viewModel.onPermissionDenied()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataSim = view.findViewById<AppCompatSpinner>(R.id.data_sim)
        val voiceSim = view.findViewById<AppCompatSpinner>(R.id.voice_sim)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.sims.filterNotNull().collect {
                        if (dataSim.adapter == null)
                            dataSim.adapter = SimsAdapter(it)

                        if (voiceSim.adapter == null)
                            voiceSim.adapter = SimsAdapter(it)
                    }
                }
                launch { viewModel.events.collect(::onEvent) }
            }
        }

        view.findViewById<Button>(R.id.btn_save)
            .setOnClickListener {
                viewModel.onSave(
                    dataSim.selectedItem as Sim,
                    voiceSim.selectedItem as Sim
                )
            }
    }

    private fun onEvent(event: SimsConfigurationEvent) {
        when (event) {
            is SimsConfigurationEvent.RequestPermission -> showPermissionSheet(event.requestCode)

            SimsConfigurationEvent.SetupSkipped -> skipSetup()
            SimsConfigurationEvent.Saved -> complete()
        }
    }

    companion object {
        fun newInstance() = SimsConfigurationFragment()
    }
}
