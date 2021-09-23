package com.smartsolutions.paquetes.ui.history

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.repositories.models.Sim


class HistoryPagerAdapter constructor(
    fragment: Fragment,
    var sims: List<Sim>
): FragmentStateAdapter(fragment) {


    override fun getItemCount(): Int {
        return sims.size
    }

    override fun createFragment(position: Int): Fragment {
        return HistoryHolderFragment.newInstance(sims[position])
    }
}