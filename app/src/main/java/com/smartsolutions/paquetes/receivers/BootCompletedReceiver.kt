package com.smartsolutions.paquetes.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.DatwallKernel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var kernel: DatwallKernel

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        kernel.mainInBackground()
    }
}