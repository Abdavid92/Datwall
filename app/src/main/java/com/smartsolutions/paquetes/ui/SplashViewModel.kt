package com.smartsolutions.paquetes.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.DatwallKernel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val kernel: DatwallKernel
) : ViewModel() {

    private var wasRunning = false

    fun main(activity: Activity) {
        if (!wasRunning) {
            wasRunning = true

            kernel.main(activity)
        }
    }
}