package com.smartsolutions.paquetes.ui

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.ActivitySplashBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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

        kernel.main(true)
    }

    override fun onResume() {
        super.onResume()
        kernel.main(true)
    }
}