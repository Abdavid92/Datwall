package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.services.FirewallService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Utilidades para el vpn
 * */
class FirewallHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

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
     * Establece en el dataStore que el cortafuegos está abilitado.
     * Este método es asincrónico.
     *
     * @param enabled
     * */
    fun establishFirewallEnabled(enabled: Boolean) {
        launch {
            context.dataStore.edit {
                it[PreferencesKeys.ENABLED_FIREWALL] = enabled
            }
        }
    }

    /**
     * Establece en el dataStore que el cortafuegos está encendido.
     * Si los datos móviles están encendidos y [IActivationManager]
     * concede el permiso de trabajo, enciende el vpn.
     *
     * @return [Intent] si el vpn no tiene permiso para encender. Si 
     * [IActivationManager] no concedió el permiso de trabajo el
     * cortafuegos no encenderá pero se retornará null en caso de que
     * el permiso del sistema esté concedido.
     * */
    fun startFirewall(): Intent? {
        establishFirewallEnabled(true)

        val intent = VpnService.prepare(context)

        val application = context.applicationContext as DatwallApplication

        if (application.dataMobileOn) {
            Log.i(TAG, "startVpn: Data mobile is on. Starting the firewall.")

            launch {
                if (intent == null && activationManager.canWork().first) {
                    startFirewallService()
                } else {
                    Log.i(TAG, "startVpn: Can not have permission by start the firewall.")
                }
            }
        } else {
            Log.i(TAG, "startVpn: Data mobile is off. For now the firewall will off.")
        }
        return intent
    }

    /**
     * Establece en el dataStore que el cortafuegos está apagado.
     * Si los datos móviles están encendidos, apaga el vpn.
     * */
    fun stopFirewall() {
        establishFirewallEnabled(false)

        val application = context.applicationContext as DatwallApplication

        if (application.dataMobileOn) {
            stopFirewallService()
        }
    }

    /**
     * Enciende el servicio del cortafuegos. Este método no realiza
     * ningún cambio en el dataStore y no revisa que los datos móbiles
     * estén encendidos.
     *
     * @return `true` si se pudo encender el cortafuegos.
     * */
    fun startFirewallService(): Boolean {

        return try {
            context.startService(Intent(context, FirewallService::class.java))

            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Apaga el servicio del cortafuegos. Este método no realiza
     * ningún cambio en el dataStore y no revisa que los datos móbiles
     * estén encendidos.
     * */
    fun stopFirewallService() {
        runCatching {
            val intent = Intent(context, FirewallService::class.java).apply {
                action = FirewallService.ACTION_STOP_FIREWALL_SERVICE
            }

            context.startService(intent)
        }
    }

    /**
     * Indica si está establecido en el dataStore que el cortafuegos está
     * encendido. El hecho de que este método retorne `true` no significa
     * que el cortafuegos está encendido ya que este se enciende y apaga
     * dependiendo de los datos móbiles.
     *
     * @return [Boolean]
     * */
    suspend fun firewallEnabled(): Boolean {
        return context.dataStore.data
            .firstOrNull()?.get(PreferencesKeys.ENABLED_FIREWALL) == true
    }

    companion object {
        private const val TAG = "FirewallHelper"
    }
}