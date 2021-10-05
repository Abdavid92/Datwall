package com.smartsolutions.paquetes.ui.settings.sim

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemSimDefaultBinding
import com.smartsolutions.paquetes.repositories.models.Sim

class DefaultSimRecyclerAdapter(
    var sims: List<Sim>
): RecyclerView.Adapter<DefaultSimRecyclerAdapter.SimHolder>() {


    class SimHolder(private val binding: ItemSimDefaultBinding): RecyclerView.ViewHolder(binding.root){

        fun onBind(sim: Sim){
            binding.apply {
                sim.apply {
                    icon?.let {
                        include.icon.setImageBitmap(it)
                    }

                    include.title.text ="Sim ${slotIndex + 1}"

                    phone?.let {
                        include.subtitle.text = it
                        include.subtitle.visibility = View.VISIBLE
                    }

                    radioButtonDataDefault.isChecked = defaultData
                    radioButtonVoiceDefault.isChecked = defaultVoice
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
                    radioButtonVoiceDefault.isClickable = false
                    radioButtonDataDefault.isClickable = false
                }else {
                    //TODO
                }
            }

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SimHolder {
      return SimHolder(ItemSimDefaultBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SimHolder, position: Int) {
        holder.onBind(sims[position])
    }

    override fun getItemCount(): Int {
       return sims.size
    }


}