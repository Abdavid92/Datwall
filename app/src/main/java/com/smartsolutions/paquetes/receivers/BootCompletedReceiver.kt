package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsolutions.paquetes.DatwallKernel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast de inicio automático y punto de entrada backend de la aplicación.
 * */
@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var kernel: DatwallKernel

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            kernel.mainInBackground()
        }
    }
}