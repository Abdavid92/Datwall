package com.smartsolutions.paquetes.watcher

import android.net.ConnectivityManager
import android.net.Network
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ChangeNetworkCallback @Inject constructor(
    private val changeNetworkHelper: IChangeNetworkHelper
): ConnectivityManager.NetworkCallback() {

    override fun onAvailable(network: Network) {
        changeNetworkHelper.setDataMobileStateOn()
    }

    override fun onLost(network: Network) {
        changeNetworkHelper.setDataMobileStateOff()
    }
}