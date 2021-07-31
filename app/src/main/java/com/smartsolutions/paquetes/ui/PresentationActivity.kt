package com.smartsolutions.paquetes.ui

import android.R.attr
import android.content.Intent
import android.widget.Toast
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST) {

            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "OK", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            }
        }
    }

}