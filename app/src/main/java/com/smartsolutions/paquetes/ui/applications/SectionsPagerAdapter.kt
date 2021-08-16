package com.smartsolutions.paquetes.ui.applications

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.smartsolutions.paquetes.R

class SectionsPagerAdapter(
    context: Context,
    fragmentManager: FragmentManager
) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val pages = arrayOf(
        Pair(USER_APPS, context.getString(R.string.page_user_apps)),
        Pair(SYSTEM_APPS, context.getString(R.string.page_system_apps))
    )

    override fun getCount(): Int {
        return pages.size
    }

    override fun getItem(position: Int): Fragment {
        return PlaceHolderFragment.newInstance(pages[position].first)
    }

    override fun getPageTitle(position: Int): CharSequence {
        return pages[position].second
    }

    companion object {
        const val USER_APPS = "user_apps"
        const val SYSTEM_APPS = "system_apps"
    }
}