package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IActivationManager2
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.services.FirewallService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Utilidades para el vpn
 * */
class FirewallHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager2
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
     * Si los datos móviles están encendidos y [IActivationManager2]
     * concede el permiso de trabajo, enciende el vpn.
     *
     * @return [Intent] si el vpn no tiene permiso para encender. Si 
     * [IActivationManager2] no concedió el permiso de trabajo el
     * cortafuegos no encenderá pero se retornará null en caso de que
     * el permiso del sistema esté concedido.
     * */
    suspend fun startFirewall(): Intent? {
        establishFirewallEnabled(true)

        var intent: Intent? = null

        if (DatwallKernel.DATA_MOBILE_ON) {
            Log.i(TAG, "startVpn: Data mobile is on. Starting the firewall.")

            if (activationManager.canWork().first) {
                intent = startFirewallService()

                if (intent != null)
                    Log.i(TAG, "startVpn: Can not have permission for start the firewall.")
            } else {
                Log.i(TAG, "startVpn: Can not have permission for start the firewall.")
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

        if (DatwallKernel.DATA_MOBILE_ON) {
            stopFirewallService()
        }
    }

    /**
     * Enciende el servicio del cortafuegos. Este método no realiza
     * ningún cambio en el dataStore y no revisa que los datos móbiles
     * estén encendidos.
     *
     * @return [Intent] si el cortafuegos no tiene permiso para encender
     * */
    fun startFirewallService(): Intent? {
        val intent: Intent? = VpnService.prepare(context)

        if (intent == null) {
            context.startService(Intent(context, FirewallService::class.java))
        }

        return intent
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

    /**
     * Observa los cambios de estados del cortafuegos (encendido, apagado)
     * El hecho de que este método retorne `true` no significa
     * que el cortafuegos está encendido ya que este se enciende y apaga
     * dependiendo de los datos móbiles.
     *
     * @return [Flow]
     * */
    fun observeFirewallState(): Flow<Boolean> {
        return context.dataStore.data
            .map {
                return@map it[PreferencesKeys.ENABLED_FIREWALL] ?: false
            }
    }

    companion object {
        private const val TAG = "FirewallHelper"
    }
}