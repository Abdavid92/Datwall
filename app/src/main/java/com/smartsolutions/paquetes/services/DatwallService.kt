package com.smartsolutions.paquetes.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.smartsolutions.paquetes.receivers.ChangeNetworkReceiver
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DatwallService : Service() {

    private val binder = DatwallServiceBinder()

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    inner class DatwallServiceBinder: Binder() {
        val service: DatwallService
            get() = this@DatwallService
    }
}