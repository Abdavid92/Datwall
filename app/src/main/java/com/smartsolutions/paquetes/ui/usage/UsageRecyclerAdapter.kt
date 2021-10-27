package com.smartsolutions.paquetes.ui.usage

import android.app.usage.NetworkStats
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemUsageBinding
import com.smartsolutions.paquetes.managers.contracts.IIconManager2
import com.smartsolutions.paquetes.managers.models.DataUnitBytes

class UsageRecyclerAdapter constructor(
    private val fragment: Fragment,
    private var apps: List<UsageApp>,
    private val iconManager: IIconManager2
): RecyclerView.Adapter<UsageRecyclerAdapter.UsageViewHolder>() {

    private var appsShow = apps.toMutableList()

    private val callBack = object : DiffUtil.Callback() {
        override fun getOldListSize(): Int {
            return appsShow.size
        }

        override fun getNewListSize(): Int {
            return this@UsageRecyclerAdapter.apps.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return appsShow[oldItemPosition].app == this@UsageRecyclerAdapter.apps[newItemPosition].app
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return appsShow[oldItemPosition].app.traffic == this@UsageRecyclerAdapter.apps[newItemPosition].app.traffic
        }
    }

    fun filter(app: List<UsageApp>){
        appsShow = if (app.isEmpty()){
            apps.toMutableList()
        }else {
            app.toMutableList()
        }
        notifyDataSetChanged()
    }


    fun updateApps(apps: List<UsageApp>){
        this.apps = apps

        val result = DiffUtil.calculateDiff(callBack, true)

        appsShow = apps.toMutableList()

        result.dispatchUpdatesTo(this)
    }

    inner class UsageViewHolder(private val binding: ItemUsageBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(usageApp: UsageApp) {
            val app = usageApp.app

            iconManager.getIcon(app.packageName, app.version) {
                binding.appIcon.setImageBitmap(it)
            }

            binding.textAppName.text = app.name
            val value = app.traffic?.totalBytes?.getValue() ?: DataUnitBytes.DataValue(0.0, DataUnitBytes.DataUnit.B)
            binding.textUsageValue.text = "${value.value} ${value.dataUnit}"
            binding.circleColour.circleColor = usageApp.colour

            binding.root.setOnClickListener {
                val dialog = UsageAppDetailsFragment.newInstance(usageApp.app)
                dialog.show(fragment.childFragmentManager, "UsageAppDetails")
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
       return UsageViewHolder(ItemUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        holder.bind(appsShow[position])
    }

    override fun getItemCount(): Int {
       return appsShow.size
    }

}