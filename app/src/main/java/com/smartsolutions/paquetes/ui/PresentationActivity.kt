package com.smartsolutions.paquetes.ui

import com.smartsolutions.paquetes.R
import com.stephentuso.welcome.*

//TODO: Los íconos están pendiente a cambios
class PresentationActivity : WelcomeActivity() {

    override fun configuration(): WelcomeConfiguration {
        return WelcomeConfiguration.Builder(this)
            .animateButtons(true)
            .backButtonNavigatesPages(true)
            .canSkip(false)
            .defaultBackgroundColor(R.color.purple_700)
            .page(TitlePage(
                R.mipmap.ic_launcher_round,
                getString(R.string.app_name)
            ))
            .page(BasicPage(
                R.mipmap.ic_launcher_round,
                getString(R.string.page_1_title),
                getString(R.string.page_1_description)
            ))
            .page(BasicPage(
                R.mipmap.ic_launcher_round,
                getString(R.string.page_2_title),
                getString(R.string.page_2_description)
            ))
            .page(BasicPage(
                R.mipmap.ic_launcher_round,
                getString(R.string.page_3_title),
                getString(R.string.page_3_description)
            ))
            .build()
    }
}