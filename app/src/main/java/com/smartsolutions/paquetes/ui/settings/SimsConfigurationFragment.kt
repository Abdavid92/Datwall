package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.widget.AppCompatSpinner
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SimsConfigurationFragment : AbstractSettingsFragment(R.layout.fragment_sims_configuration) {

    private val viewModel by viewModels<SimsConfigurationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataSim = view.findViewById<AppCompatSpinner>(R.id.data_sim)
        val voiceSim = view.findViewById<AppCompatSpinner>(R.id.voice_sim)

        viewModel.getSims(this, childFragmentManager).observe(viewLifecycleOwner) {
            dataSim.adapter = SimsAdapter(it)
            voiceSim.adapter = SimsAdapter(it)
        }

        view.findViewById<Button>(R.id.btn_save)
            .setOnClickListener {
                viewModel.saveChanges(
                    dataSim.selectedItem as Sim,
                    voiceSim.selectedItem as Sim
                ){
                    complete()
                }
            }
    }

    companion object {
        fun newInstance() = SimsConfigurationFragment()
    }
}