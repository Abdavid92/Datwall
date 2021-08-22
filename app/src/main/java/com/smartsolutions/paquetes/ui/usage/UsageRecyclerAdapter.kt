package com.smartsolutions.paquetes.ui.usage

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.databinding.ItemUsageBinding
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.App
import java.time.Period
import kotlin.math.roundToInt

class UsageRecyclerAdapter constructor(
    private var apps: List<UsageApp>,
    private val iconManager: IIconManager
): RecyclerView.Adapter<UsageRecyclerAdapter.UsageViewHolder>() {

    private var appsShow = apps.toMutableList()


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
        appsShow = apps.toMutableList()
        notifyDataSetChanged()
    }

    inner class UsageViewHolder(private val binding: ItemUsageBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(usageApp: UsageApp) {
            val app = usageApp.app
            iconManager.getAsync(app.packageName, app.version) {
                binding.appIcon.setImageBitmap(it)
            }

            binding.textAppName.text = app.name
            val value = app.traffic?.totalBytes?.getValue() ?: DataUnitBytes.DataValue(0.0, DataUnitBytes.DataUnit.B)
            binding.textUsageValue.text = "${value.value.roundToInt()} ${value.dataUnit}"
            binding.circleColour.circleColor = usageApp.colour
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