package com.smartsolutions.paquetes.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StyleRes
import androidx.recyclerview.widget.RecyclerView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ItemThemeBinding
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.managers.models.ThemeWrapper

class ThemesAdapter(
    private val uiHelper: UIHelper,
    @StyleRes
    private var currentThemeId: Int
) : RecyclerView.Adapter<ThemesAdapter.ViewHolder>() {

    private val themeList = uiHelper.getThemeList()

    var onThemeChange: ((themeId: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemThemeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(themeList[position])
    }

    override fun getItemCount(): Int {
        return themeList.size
    }

    inner class ViewHolder(
        private val binding: ItemThemeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(themeWrapper: ThemeWrapper) {
            uiHelper.getColorTheme(R.attr.colorPrimary, themeWrapper.theme)?.let {
                binding.cardPrimary.setCardBackgroundColor(it)
            }

            uiHelper.getColorTheme(R.attr.colorSecondary, themeWrapper.theme)?.let {
                binding.cardSecondary.circleColor = it
            }

            binding.checked = themeWrapper.id == currentThemeId

            binding.cardPrimary.setOnClickListener {
                currentThemeId = themeWrapper.id
                onThemeChange?.invoke(currentThemeId)
                notifyDataSetChanged()
            }
        }
    }
}