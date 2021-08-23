package com.smartsolutions.paquetes.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ProgressBar
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.stephentuso.welcome.WelcomeHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Actividad de inicio y punto de entrada frontend de la aplicación.
 * */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity(R.layout.activity_splash) {

    /*@Inject
    lateinit var kernel: DatwallKernel*/

    private val viewModel by viewModels<SplashViewModel>()

    private val progressBar by findView<ProgressBar>(R.id.progressBar)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(
            WindowInsetsCompat.Type.systemBars()
        )
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val handler = Handler(Looper.getMainLooper())

        /*Después de 1000 milisegundos intento iniciar la PresentationActivity.
        * Si tiene éxito, espero el resultado en el onActivityResult. Sino,
        * inicio el kernel.*/
        handler.postDelayed({
            if (!WelcomeHelper(this, PresentationActivity::class.java)
                    .show(savedInstanceState)) {
                progressBar.animate()
                    .alpha(1F)
                    .duration = 200
                //kernel.mainInForeground(this)
                viewModel.main(this)
            }
        }, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST) {
            progressBar.animate()
                .alpha(1F)
                .duration = 200
            //kernel.mainInForeground(this)
            viewModel.main(this)
        }
    }

    private fun <T : View?> findView(resId: Int) = lazy {
        findViewById<T>(resId)
    }
}