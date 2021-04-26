package com.smartsolutions.paquetes.firewall

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.services.FirewallService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Utilidades para el vpn
 * */
object VpnConnectionUtils {

    private val TAG = "VpnConnectionUtils"

    /**
     * Verifica si todas las aplicaciones de la lista tienen acceso
     *
     * @return true si todas tienen acceso, false en caso contrario
     * */
    fun allAccess(apps: List<IApp>): Boolean {
        apps.forEach {
            if (it is App && !it.access && !it.tempAccess) {
                return false
            } else if (it is AppGroup) {
                it.forEach { app ->
                    if (!app.access && !app.tempAccess)
                        return false
                }
            }
        }
        return true
    }

    /**
     * Establece en el dataStore que el cortafuegos está encendido.
     * Si los datos móviles están encendidos, enciende el vpn.
     *
     * @return Intent si el vpn no tiene permiso para encender.
     * */
    fun startVpn(context: Context): Intent? {
        val intent = VpnService.prepare(context)

        GlobalScope.launch {
            context.dataStore.edit {
                it[PreferencesKeys.FIREWALL_ON] = true
            }

            context.dataStore.data.firstOrNull()?.let {
                if (it[PreferencesKeys.DATA_MOBILE_ON] == true) {

                    Log.i(TAG, "startVpn: Data mobile is on. Starting the firewall.")

                    if (intent == null) {
                        context.startService(Intent(context, FirewallService::class.java))
                    } else {
                        Log.i(TAG, "startVpn: Can not have permission by start the firewall.")
                    }
                } else {
                    Log.i(TAG, "startVpn: Data mobile is off. For now the firewall will off.")
                }
            }
        }
        return intent
    }

    /**
     * Establece en el dataStore que el cortafuegos está apagado.
     * Si los datos móviles están encendidos, apaga el vpn.
     * */
    fun stopVpn(context: Context) {
        GlobalScope.launch {
            context.dataStore.edit {
                it[PreferencesKeys.FIREWALL_ON] = false
            }

            context.dataStore.data.firstOrNull()?.let {
                if (it[PreferencesKeys.DATA_MOBILE_ON] == true) {

                    val intent = Intent(context, FirewallService::class.java).apply {
                        action = FirewallService.ACTION_STOP_FIREWALL_SERVICE
                    }

                    context.startService(intent)
                }
            }
        }
    }
}