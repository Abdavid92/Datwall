package com.smartsolutions.paquetes.ui.applications

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentPagerAdapter

class SectionsPagerAdapter(
    fragment: Fragment
) : FragmentPagerAdapter(fragment.childFragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    override fun getCount(): Int {
        return 2
    }

    override fun getItem(position: Int): Fragment {
        return PlaceHolderFragment.newInstance()
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Usuario"
            1 -> "Sistema"
            else -> null
        }
    }
}