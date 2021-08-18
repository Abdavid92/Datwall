package com.smartsolutions.paquetes.ui.usage

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.R

class FragmentPageAdapter constructor(
    context: Context,
    fragment: Fragment
): FragmentStateAdapter(fragment) {

    val fragmentsList = listOf(
        Pair(INTERNATIONAL_FRAGMENT, context.getString(R.string.tab_international)),
        Pair(NATIONAL_FRAGMENT, context.getString(R.string.tab_national)),
        Pair(FREE_FRAGMENT, context.getString(R.string.tab_free))
    )

    override fun getItemCount(): Int {
       return fragmentsList.size
    }

    override fun createFragment(position: Int): Fragment {
        return UsageHolderFragment.newInstance(fragmentsList[position].first)
    }


    companion object {
        const val INTERNATIONAL_FRAGMENT = "international_fragment"
        const val NATIONAL_FRAGMENT = "national_fragment"
        const val FREE_FRAGMENT = "free_fragment"
    }
}