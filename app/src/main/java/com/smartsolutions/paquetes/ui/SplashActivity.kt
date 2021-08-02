package com.smartsolutions.paquetes.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.stephentuso.welcome.WelcomeHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Actividad de inicio y punto de entrada frontend de la aplicación.
 * */
@AndroidEntryPoint
class SplashActivity : AppCompatActivity(R.layout.activity_splash) {

    @Inject
    lateinit var kernel: DatwallKernel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(
            WindowInsetsCompat.Type.systemBars()
        )

        val handler = Handler(Looper.getMainLooper())

        /*Después de 1500 milisegundos intento iniciar la PresentationActivity.
        * Si tiene éxito, espero el resultado en el onActivityResult. Sino,
        * inicio el kernel.*/
        handler.postDelayed({
            if (!WelcomeHelper(this, PresentationActivity::class.java)
                    .show(savedInstanceState)) {
                kernel.mainInForeground(this)
            }
        }, 1500)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST) {
            kernel.mainInForeground(this)
        }
    }
}