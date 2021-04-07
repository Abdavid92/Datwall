package com.smartsolutions.datwall.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class ChangeNetworkReceiverLegacy : BaseNetworkReceiver() {
    val runnable = Runnable {

    }

    override fun onReceive(context: Context, intent: Intent) {
        intent.extras?.let { extra ->
            extra.getParcelable<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)?.let {
                launchConnectivityEvents(it.type == ConnectivityManager.TYPE_MOBILE && it.isConnected, context)
            }
        }
    }
}



abstract class BaseNetworkReceiver : BroadcastReceiver(){
    protected fun launchConnectivityEvents(dataMobile: Boolean, context: Context){
        Toast.makeText(context, "Status $dataMobile", Toast.LENGTH_SHORT).show()
    }
}