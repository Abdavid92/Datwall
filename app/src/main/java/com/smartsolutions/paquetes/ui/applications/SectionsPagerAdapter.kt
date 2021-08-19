package com.smartsolutions.paquetes.ui.applications

import androidx.annotation.StringDef
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.smartsolutions.paquetes.R

class SectionsPagerAdapter(
    fragment: Fragment
) : FragmentStateAdapter(fragment) {

    val pages = arrayOf(
        Pair(USER_APPS, fragment.getString(R.string.page_user_apps)),
        Pair(SYSTEM_APPS, fragment.getString(R.string.page_system_apps))
    )

    companion object {
        const val USER_APPS = "user_apps"
        const val SYSTEM_APPS = "system_apps"
    }

    override fun getItemCount() = pages.size

    override fun createFragment(position: Int): Fragment =
        PlaceHolderFragment.newInstance(pages[position].first)
}

@StringDef(SectionsPagerAdapter.USER_APPS, SectionsPagerAdapter.SYSTEM_APPS)
@Retention(AnnotationRetention.SOURCE)
annotation class FragmentAppsKey