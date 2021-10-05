package com.smartsolutions.paquetes.ui.packages

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.repositories.models.Sim

class PackagesPagerAdapter(
    var sims: List<Sim>,
    fragment: Fragment
): FragmentStateAdapter(fragment) {


    override fun getItemCount(): Int {
        return sims.size
    }

    override fun createFragment(position: Int): Fragment {
       return PackagesHolderFragment.newInstance(sims[position].id)
    }
}