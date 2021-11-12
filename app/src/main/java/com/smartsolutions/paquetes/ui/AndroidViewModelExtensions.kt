package com.smartsolutions.paquetes.ui

import android.app.Activity
import android.os.Bundle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import com.smartsolutions.paquetes.DatwallApplication

fun AndroidViewModel.next() {
    getApplication<DatwallApplication>().main()
}

fun AndroidViewModel.addOpenActivityListener(
    lifecycleOwner: LifecycleOwner,
    listener: (
        activity: Class<out Activity>,
        args: Bundle?,
        application: DatwallApplication
    ) -> Unit
) {
    getApplication<DatwallApplication>()
        .addOpenActivityListener(lifecycleOwner, listener)
}