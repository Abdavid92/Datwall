package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Handler
import android.os.Looper
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import javax.inject.Inject

/**
 * Receptor que se usa para observar los cambio de redes.
 * Se encarga de mandar a cambiar el estado de la aplicación dependiendo del estado
 * de la red. Solo observará el cambio de las redes móbiles. Este receptor solo se
 * usa en las apis 21 y 22. En las demas apis se usará un callback.
 * */
class ChangeNetworkReceiver @Inject constructor(
    private val changeNetworkHelper: IChangeNetworkHelper
) : BroadcastReceiver() {

    /**
     * Instancia de NetworkInfo que contiene la información relacionada con
     * la red que cambió.
     * */
    private var networkInfo: NetworkInfo? = null

    private var isDelayed = false

    override fun onReceive(context: Context, intent: Intent) {
        //Extra del intent
        intent.getParcelableExtra<NetworkInfo>(ConnectivityManager.EXTRA_NETWORK_INFO)?.let {
            //Si el tipo de red que cambió es TYPE_MOBILE
            if (it.type == ConnectivityManager.TYPE_MOBILE) {
                //Asigno el networkInfo sacado del intent
                networkInfo = it

                if (!isDelayed) {
                    isDelayed = true
                    /*
                    * Este Handler se usa para poder quedarse con el último networkInfo
                    * que se obtuvo ya que este broadcast se lanza varias veces. Para evitar
                    * inestabilidad en la aplicación se espera a que deje de lanzarse el broadcast y
                    * se guarda el último intent.
                    * */
                    Handler(Looper.getMainLooper())
                        .postDelayed({
                            networkInfo?.let { networkInfo ->

                                /*
                                * Una vez se logre ejecutar la tarea programada en el Handler
                                * se procede a cambiar el estado de la aplicación dependiendo
                                * de la red movil.
                                * */
                                if (networkInfo.isConnected) {
                                    changeNetworkHelper.setDataMobileStateOn()
                                } else {
                                    changeNetworkHelper.setDataMobileStateOff()
                                }

                                isDelayed = false
                                this@ChangeNetworkReceiver.networkInfo = null
                            }
                        }, 500)
                }
            }
        }
    }
}
