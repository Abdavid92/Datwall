package com.smartsolutions.paquetes.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.smartsolutions.paquetes.R
import com.stephentuso.welcome.WelcomeHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Actividad de inicio y punto de entrada frontend de la aplicación.
 * */
@AndroidEntryPoint
class SplashActivity : AbstractActivity(R.layout.activity_splash), CoroutineScope {

    private val viewModel by viewModels<SplashViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.show(
            WindowInsetsCompat.Type.systemBars()
        )

        launch {
            delay(1000)

            if (withContext(Dispatchers.IO) {
                    !WelcomeHelper(this@SplashActivity, PresentationActivity::class.java)
                        .show(savedInstanceState)
                }) {

                withContext(Dispatchers.Main) {
                    viewModel.addOpenActivityListener(this@SplashActivity) { activity, application ->
                        application.removeOpenActivityListener(this@SplashActivity)
                        startActivity(Intent(this@SplashActivity, activity))
                        finish()
                    }
                }
            }
        }

    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == WelcomeHelper.DEFAULT_WELCOME_SCREEN_REQUEST) {

            viewModel.addOpenActivityListener(this) { activity, application ->
                application.removeOpenActivityListener(this)
                startActivity(Intent(this, activity))
                finish()
            }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}