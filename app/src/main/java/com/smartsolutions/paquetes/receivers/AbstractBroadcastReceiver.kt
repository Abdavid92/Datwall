package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter

abstract class AbstractBroadcastReceiver : BroadcastReceiver() {

    var isRegister = false
        private set

    fun register(context: Context, filter: IntentFilter) {
        if (!isRegister) {
            context.registerReceiver(this, filter)
            isRegister = true
        }
    }

    fun unregister(context: Context) {
        context.unregisterReceiver(this)
        isRegister = false
    }
}