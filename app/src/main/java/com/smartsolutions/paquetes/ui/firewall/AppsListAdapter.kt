package com.smartsolutions.paquetes.ui.firewall

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.AppItemBinding
import com.smartsolutions.paquetes.managers.IconManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp

class AppsListAdapter(
    private val list: List<IApp>,
    private val iconManager: IconManager
): RecyclerView.Adapter<AppsListAdapter.ViewHolder>() {

    var onAccessChange: ((app: IApp) -> Unit)? = null

    override fun onCreateViewHolder(p0: ViewGroup, p1: Int) =
        ViewHolder(DataBindingUtil.inflate(
            LayoutInflater.from(p0.context),
            R.layout.app_item,
            p0,
            false
        ))

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        p0.bind(list[p1])
    }

    override fun getItemCount() = list.size

    inner class ViewHolder(
        private val binding: AppItemBinding,
        ): RecyclerView.ViewHolder(binding.root) {

        fun bind(app: IApp) {
            binding.name.text = app.name
            if (app is App)
                binding.icon.setImageBitmap(iconManager.get(app.packageName, app.version.toString()))

            binding.access.setOnCheckedChangeListener(null)
            binding.access.isChecked = app.access
            binding.access.setOnCheckedChangeListener { _, isChecked ->
                app.access = isChecked
                onAccessChange?.invoke(app)
            }
        }

    }
}