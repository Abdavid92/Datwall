package com.smartsolutions.paquetes.ui

import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.smartsolutions.paquetes.kernel.DatwallApplication

fun AndroidViewModel.next() {
    getApplication<DatwallApplication>().main()
}

fun AndroidViewModel.addOpenActivityListener(
    lifecycleOwner: LifecycleOwner,
    listener: (activity: Class<out Activity>) -> Unit
) {
    getApplication<DatwallApplication>()
        .addOpenActivityListener(lifecycleOwner, listener)
}