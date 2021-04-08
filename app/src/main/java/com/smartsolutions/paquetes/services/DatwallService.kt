package com.smartsolutions.paquetes.services

import android.app.Service
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.smartsolutions.datwall.receivers.ChangeNetworkReceiver
import dagger.Lazy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DatwallService : Service() {

    private val binder = DatwallServiceBinder()

    @Inject
    lateinit var changeNetworkReceiver: Lazy<ChangeNetworkReceiver>

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val filter = IntentFilter()
            filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)

            registerReceiver(changeNetworkReceiver.get(), filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {



        return super.onStartCommand(intent, flags, startId)
    }



    override fun onDestroy() {
        super.onDestroy()

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            unregisterReceiver(changeNetworkReceiver.get())
        }
    }

    inner class DatwallServiceBinder: Binder() {
        val service: DatwallService
            get() = this@DatwallService
    }
}