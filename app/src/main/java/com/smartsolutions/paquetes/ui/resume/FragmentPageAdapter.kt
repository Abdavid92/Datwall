package com.smartsolutions.paquetes.ui.resume

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.repositories.models.Sim

class FragmentPageAdapter constructor(
    fragment: ResumeFragment,
    var sims: List<Sim>
): FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return sims.size
    }

    override fun createFragment(position: Int): Fragment {
        return ResumeHolderFragment.newInstance(sims[position].id)
    }
}