package com.smartsolutions.paquetes.ui

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.smartsolutions.paquetes.DatwallKernel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val kernel: DatwallKernel
) : ViewModel() {

    fun addOpenActivityListener(
        lifecycleOwner: LifecycleOwner,
        listener: (activity: Class<out Activity>) -> Unit
    ) {
        kernel.addOpenActivityListener(lifecycleOwner, listener)
    }
}