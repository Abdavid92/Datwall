package com.smartsolutions.paquetes.ui.usage

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.TrafficType

class FragmentPageAdapter constructor(
    fragment: Fragment
): FragmentStateAdapter(fragment) {

    val fragmentsList = listOf(
        Pair(TrafficType.International, fragment.getString(R.string.tab_international)),
        Pair(TrafficType.National, fragment.getString(R.string.tab_national)),
        Pair(TrafficType.Messaging, fragment.getString(R.string.tab_messaging)),
        Pair(TrafficType.Free, fragment.getString(R.string.tab_free))
    )

    override fun getItemCount(): Int {
       return fragmentsList.size
    }

    override fun createFragment(position: Int): Fragment {
        return UsageHolderFragment.newInstance(fragmentsList[position].first)
    }

}