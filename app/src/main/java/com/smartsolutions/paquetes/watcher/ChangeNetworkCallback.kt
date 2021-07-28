package com.smartsolutions.paquetes.watcher

import android.net.ConnectivityManager
import android.net.Network
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Callback que contiene eventos que se llaman cuando hay un cambio de red.
 * Se registra solo en la api 23 en adelante. En api 22 y 21 se usa un receiver para
 * cumlir el mismo objetivo. Esta envuelto en una instancia de Lazy para no inyectarlo innecesariamente
 * cuando no se vaya a registrar.
 * */
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