package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.workers.TrafficRegistration
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.services.FirewallService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Implementación de la interfaz IChangeNetworkHelper.
 * */
class ChangeNetworkHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val trafficRegistration: TrafficRegistration,
    private val networkUtil: NetworkUtil
): CoroutineScope, IChangeNetworkHelper {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    /**
     * Indica si el cortafuegos está encendido.
     * */
    private var firewallOn =  false

    init {

        launch {
            context.dataStore.data.collect {
                firewallOn = it[PreferencesKeys.ENABLED_FIREWALL] == true
            }
        }
    }

    override fun setDataMobileStateOn() {
        (context as DatwallApplication).dataMobileOn = true

        if (firewallOn) {
            val intent = Intent(context, FirewallService::class.java)

            context.startService(intent)
        }
        trafficRegistration.startRegistration()

        if (networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G) {
            launch {
                context.dataStore.edit {
                    it[PreferencesKeys.ENABLED_LTE] = true
                }
            }
        }
    }

    override fun setDataMobileStateOff() {
        (context as DatwallApplication).dataMobileOn = false

        if (firewallOn) {
            val intent = Intent(context, FirewallService::class.java)
                .setAction(FirewallService.ACTION_STOP_FIREWALL_SERVICE)

            context.startService(intent)
        }
        trafficRegistration.stopRegistration()
    }
}