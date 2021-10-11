package com.smartsolutions.paquetes.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.DatwallKernel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val kernel: DatwallKernel
) : ViewModel() {

    fun launchActivity() = kernel.nextActivity
}