package com.smartsolutions.paquetes.receivers

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsolutions.paquetes.DatwallKernel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast de inicio automático y punto de entrada backend de la aplicación.
 * */
class BootCompletedReceiver : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) { }
}