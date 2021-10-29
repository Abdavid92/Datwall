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
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class UsageRecyclerAdapter constructor(
    private val fragment: Fragment,
    private var apps: List<UsageApp>,
    private val iconManager: IIconManager2
): RecyclerView.Adapter<UsageRecyclerAdapter.UsageViewHolder>(), CoroutineScope {

    private var appsShow = apps.toMutableList()

    private val callBack get() = object : DiffUtil.Callback() {
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
        launch {
            this@UsageRecyclerAdapter.apps = apps

            val result = DiffUtil.calculateDiff(callBack, true)

            appsShow = apps.toMutableList()

            withContext(Dispatchers.Main) {
                result.dispatchUpdatesTo(this@UsageRecyclerAdapter)
            }
        }
    }

    inner class UsageViewHolder(var binding: ItemUsageBinding?): RecyclerView.ViewHolder(binding!!.root) {

        var job: Job? = null

        fun bind(usageApp: UsageApp) {
            val app = usageApp.app

            binding?.apply {
                job = iconManager.getIcon(app.packageName, app.version) {
                    appIcon.setImageBitmap(it)
                }

                textAppName.text = app.name
                val value = app.traffic?.totalBytes?.getValue() ?: DataUnitBytes.DataValue(0.0, DataUnitBytes.DataUnit.B)
                textUsageValue.text = "${value.value} ${value.dataUnit}"
                circleColour.circleColor = usageApp.colour

                root.setOnClickListener {
                    val dialog = UsageAppDetailsFragment.newInstance(usageApp.app)
                    dialog.show(fragment.childFragmentManager, "UsageAppDetails")
                }
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsageViewHolder {
       return UsageViewHolder(ItemUsageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: UsageViewHolder, position: Int) {
        holder.bind(appsShow[position])
    }

    override fun onViewRecycled(holder: UsageViewHolder) {
        holder.job?.cancel()
        holder.job = null
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
       return appsShow.size
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

}