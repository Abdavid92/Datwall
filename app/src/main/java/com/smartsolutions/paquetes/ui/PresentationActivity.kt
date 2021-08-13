package com.smartsolutions.paquetes.ui

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.R
import com.stephentuso.welcome.*

class PresentationActivity : WelcomeActivity() {

    override fun configuration(): WelcomeConfiguration {

        return WelcomeConfiguration.Builder(this)
            .animateButtons(true)
            .backButtonNavigatesPages(true)
            .canSkip(false)
            .defaultBackgroundColor(R.color.color_primary)
            .useCustomDoneButton(false)
            .page(object : FragmentWelcomePage() {
                override fun fragment(): Fragment {
                    return WelcomeTitleFragment()
                }
            })
            .page(
                BasicPage(
                    R.drawable.presentation_page_1,
                    getString(R.string.page_1_title),
                    getString(R.string.page_1_description)
                )
            )
            .page(
                BasicPage(
                    R.drawable.presentation_page_2,
                    getString(R.string.page_2_title),
                    getString(R.string.page_2_description)
                )
            )
            .page(
                BasicPage(
                    R.drawable.presentation_page_3,
                    getString(R.string.page_3_title),
                    getString(R.string.page_3_description)
                )
            )
            .build()
    }

    class WelcomeTitleFragment :
        Fragment(com.stephentuso.welcome.R.layout.wel_fragment_title),
        WelcomePage.OnChangeListener {

        private lateinit var image: ImageView
        private lateinit var title: TextView

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            image = view.findViewById(com.stephentuso.welcome.R.id.wel_image)
            title = view.findViewById(com.stephentuso.welcome.R.id.wel_title)

            image.setImageResource(R.drawable.presentation_page_0)
            title.text = HtmlCompat.fromHtml(
                getString(R.string.page_0_title),
                HtmlCompat.FROM_HTML_MODE_COMPACT)
        }

        override fun onWelcomeScreenPageScrolled(pageIndex: Int, offset: Float, offsetPixels: Int) {
            image.translationX = -offsetPixels * 0.8f
        }

        override fun onWelcomeScreenPageSelected(pageIndex: Int, selectedPageIndex: Int) {
            //Empty
        }

        override fun onWelcomeScreenPageScrollStateChanged(pageIndex: Int, state: Int) {
            //Empty
        }
    }
}