package com.smartsolutions.datwall.watcher

import android.net.ConnectivityManager
import android.net.Network
import com.smartsolutions.datwall.helpers.IChangeNetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class ChangeNetworkCallback @Inject constructor(
    private val changeNetworkHelper: IChangeNetworkHelper
): ConnectivityManager.NetworkCallback(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun onAvailable(network: Network) {
        changeNetworkHelper.setDataMobileStateOn()
    }

    override fun onLost(network: Network) {
        changeNetworkHelper.setDataMobileStateOff()
    }
}