package com.smartsolutions.paquetes.ui.usage

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.TrafficType
import dagger.hilt.android.AndroidEntryPoint

class FragmentPageAdapter constructor(
    context: Context,
    fragment: Fragment
): FragmentStateAdapter(fragment) {

    val fragmentsList = listOf(
        Pair(TrafficType.International, context.getString(R.string.tab_international)),
        Pair(TrafficType.National, context.getString(R.string.tab_national)),
        Pair(TrafficType.Free, context.getString(R.string.tab_free))
    )

    override fun getItemCount(): Int {
       return fragmentsList.size
    }

    override fun createFragment(position: Int): Fragment {
        return UsageHolderFragment.newInstance(fragmentsList[position].first)
    }

}