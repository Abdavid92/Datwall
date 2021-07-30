package com.smartsolutions.paquetes.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
    }

    override fun onResume() {
        super.onResume()
        kernel.mainInForeground(true)
    }
}