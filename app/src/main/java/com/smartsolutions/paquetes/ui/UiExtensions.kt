package com.smartsolutions.paquetes.ui

import android.content.Context
import android.view.LayoutInflater
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.smartsolutions.paquetes.databinding.TabItemBinding
import com.smartsolutions.paquetes.helpers.DisplayTexts
import com.smartsolutions.paquetes.helpers.UIHelper
import com.smartsolutions.paquetes.repositories.models.Sim

/**
 * Pestañas con el icono y el nombre de cada línea.
 * */
fun setTabLayoutMediatorSims(
    context: Context,
    tabLayout: TabLayout,
    pager2: ViewPager2,
    sims: List<Sim>
): TabLayoutMediator? {
    return try {
        TabLayoutMediator(tabLayout, pager2) { tab, pos ->
            val tabBind =
                TabItemBinding.inflate(LayoutInflater.from(context), null, false)
            val sim = sims.getOrNull(pos) ?: return@TabLayoutMediator

            sim.icon?.let {
                tabBind.icon.setImageBitmap(it)
            }

            tabBind.title.text = DisplayTexts.simName(sim, context)

            tab.customView = tabBind.root

            tab.view.setOnLongClickListener {
                true
            }
        }.also { it.attach() }
    } catch (e: Exception) {
        null
    }
}

/**
 * Crea una instancia [Lazy] de [UIHelper]
 *
 * Ej: val uiHelper by uiHelper()
 * */
fun Fragment.uiHelper(): Lazy<UIHelper> {
    return lazy {
        UIHelper(requireContext())
    }
}
