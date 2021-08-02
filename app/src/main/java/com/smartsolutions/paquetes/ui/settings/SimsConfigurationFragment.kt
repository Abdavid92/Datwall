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
class SimsConfigurationFragment @Inject constructor(

): AbstractSettingsFragment(R.layout.fragment_sims_configuration) {

    private val viewModel by viewModels<SimsConfigurationViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dataSim = view.findViewById<AppCompatSpinner>(R.id.data_sim)
        val voiceSim = view.findViewById<AppCompatSpinner>(R.id.voice_sim)

        viewModel.getSims(this, childFragmentManager).observe(viewLifecycleOwner) {
            dataSim.adapter = Adapter(it)
            voiceSim.adapter = Adapter(it)
        }

        view.findViewById<Button>(R.id.btn_save)
            .setOnClickListener {
                viewModel.saveChanges(
                    dataSim.selectedItem as Sim,
                    voiceSim.selectedItem as Sim
                )
                complete()
            }
    }

    companion object {
        fun newInstance() = SimsConfigurationFragment()
    }

    class Adapter(
        private val sims: List<Sim>
    ) : BaseAdapter() {

        override fun getCount(): Int {
            return sims.size
        }

        override fun getItem(position: Int): Sim {
            return sims[position]
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val layoutInflater = LayoutInflater.from(parent.context)

            val view = convertView ?: layoutInflater
                .inflate(R.layout.sim_spinner_item, parent, false)

            bindView(view, position)

            return view
        }

        private fun bindView(view: View, position: Int) {
            val icon = view.findViewById<ImageView>(R.id.sim_icon)
            val name = view.findViewById<TextView>(R.id.sim_name)

            val sim = getItem(position)

            icon.setImageBitmap(sim.icon)
            name.text = sim.phone ?: "Sim ${position + 1 }"
        }
    }
}