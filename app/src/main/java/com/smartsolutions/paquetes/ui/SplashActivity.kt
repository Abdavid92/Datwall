package com.smartsolutions.paquetes.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowInsets
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


        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        if (Build.VERSION.SDK_INT >= 30) {
            windowInsetsController.show(
                WindowInsetsCompat.Type.systemBars()
            )
        } else {
            progressBar.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }

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

            viewModel.main(this)
        }
    }

    private fun <T : View?> findView(resId: Int) = lazy {
        findViewById<T>(resId)
    }
}