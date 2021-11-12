package com.smartsolutions.paquetes.ui.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.Sim

class SimsAdapter(
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

        if (convertView != null)
            return convertView

        val layoutInflater = LayoutInflater.from(parent.context)

        val view = convertView ?: layoutInflater
            .inflate(R.layout.item_sim_spinner, parent, false)

        bindView(view, position)

        return view
    }

    private fun bindView(view: View, position: Int) {
        val icon = view.findViewById<ImageView>(R.id.sim_icon)
        val name = view.findViewById<TextView>(R.id.sim_name)

        val sim = getItem(position)

        icon.setImageBitmap(sim.icon)
        name.text = "Sim ${sim.slotIndex + 1}"
    }
}