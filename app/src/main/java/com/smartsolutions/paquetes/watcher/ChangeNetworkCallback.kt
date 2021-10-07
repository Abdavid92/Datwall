package com.smartsolutions.paquetes.watcher

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import javax.inject.Inject

/**
 * Callback que contiene eventos que se llaman cuando hay un cambio de red.
 * Se registra solo en la api 23 en adelante. En api 22 y 21 se usa un receiver para
 * cumlir el mismo objetivo. Esta envuelto en una instancia de Lazy para no inyectarlo innecesariamente
 * cuando no se vaya a registrar.
 * */
class ChangeNetworkCallback @Inject constructor(
    private val changeNetworkHelper: IChangeNetworkHelper
): ConnectivityManager.NetworkCallback() {

    /**
     * Indica si el callback ya fué registrado.
     * */
    var isRegistered = false
        private set

    override fun onAvailable(network: Network) {
        changeNetworkHelper.setDataMobileStateOn()
    }

    override fun onLost(network: Network) {
        changeNetworkHelper.setDataMobileStateOff()
    }

    fun register(connectivityManager: ConnectivityManager) {
        if (!isRegistered) {
            /*El Transport del request es de tipo cellular para escuchar los cambios de
             * redes móbiles solamente.*/
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)

            connectivityManager.registerNetworkCallback(request.build(), this)
            isRegistered = true
        }
    }

    fun unregister(connectivityManager: ConnectivityManager) {
        connectivityManager.unregisterNetworkCallback(this)
        isRegistered = false
    }
}