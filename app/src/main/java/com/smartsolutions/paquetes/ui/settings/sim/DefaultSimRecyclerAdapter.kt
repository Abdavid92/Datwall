package com.smartsolutions.paquetes.ui.settings.sim

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemSimDefault2Binding
import com.smartsolutions.paquetes.databinding.ItemSimDefaultBinding
import com.smartsolutions.paquetes.repositories.models.Sim

class DefaultSimRecyclerAdapter(
    private var fragment: DefaultSimsDialogFragment,
    var sims: List<Sim>,
    var isDefaultVoice: Boolean
) : RecyclerView.Adapter<DefaultSimRecyclerAdapter.SimHolder>() {


    inner class SimHolder(private val binding: ItemSimDefault2Binding) :
        RecyclerView.ViewHolder(binding.root) {

        fun onBind(sim: Sim) {
            binding.apply {
                sim.apply {

                    if (isDefaultVoice) {
                        radioButton.isChecked = defaultVoice
                    } else {
                        radioButton.isChecked = defaultData
                    }

                    icon?.let {
                        iconSim.setImageBitmap(it)
                    }
                    textSim.text = "Sim ${sim.slotIndex + 1}"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    radioButton.isClickable = false
                } else {
                    radioButton.setOnCheckedChangeListener { buttonView, isChecked ->
                        fragment.setDefaultSim(sim, isDefaultVoice)
                    }
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimHolder {
        return SimHolder(
            ItemSimDefault2Binding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SimHolder, position: Int) {
        holder.onBind(sims[position])
    }

    override fun getItemCount(): Int {
        return sims.size
    }


}