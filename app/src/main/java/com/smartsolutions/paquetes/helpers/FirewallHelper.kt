package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.services.FirewallService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Utilidades para el vpn
 * */
class FirewallHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val activationManager: IActivationManager,
    private val permissionManager: IPermissionsManager
) {

    private val dataStore = context.internalDataStore

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
     * Si los datos móviles están encendidos y [IActivationManager]
     * concede el permiso de trabajo, enciende el vpn. Si
     * [IActivationManager] no concedió el permiso de trabajo el
     * cortafuegos no encenderá. Si no se concedió el permiso necesario
     * el cortafuegos no encenderá, se apagará en el dataStore y se
     * enviará una notificación informando.
     *
     * @param persistent - Si está en `true` se enciende permanentemente en el dataStore
     * */
    suspend fun startFirewall(persistent: Boolean) {

        if (persistent)
            establishFirewallEnabled(true)

        if (DatwallKernel.DATA_MOBILE_ON) {
            Log.i(TAG, "startVpn: Data mobile is on. Starting the firewall.")

            if (activationManager.canWork().first) {
                if (!startFirewallService()) {
                    Log.i(TAG, "startVpn: Can not have permission for start the firewall.")
                    establishFirewallEnabled(false)

                    notify(
                        R.string.stoped_missing_vpn_permissions_title_notification,
                        R.string.stoped_missing_vpn_permissions_description_notification
                    )
                }

            } else {
                Log.i(TAG, "startVpn: Can not have permission for start the firewall.")
            }
        } else {
            Log.i(TAG, "startVpn: Data mobile is off. For now the firewall is off.")
        }
    }

    /**
     * Apaga el cortafuegos.
     *
     * @param persistent - Si esta en `true` se desactiva el cortafuegos permanentemente.
     * */
    suspend fun stopFirewall(persistent: Boolean) {

        if (persistent)
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
     * @return `false` si el cortafuegos no tiene permiso para encender
     * */
    private fun startFirewallService(): Boolean {

        if (!checkFirewallPermission()) {
            return false
        }

        context.startService(Intent(context, FirewallService::class.java))

        return true
    }

    fun checkFirewallPermission(): Boolean {
        val permission = permissionManager.findPermission(IPermissionsManager.VPN_CODE)
            ?: throw IllegalArgumentException("Bad code")

        return permission.checkPermission(permission, context)
    }

    /**
     * Apaga el servicio del cortafuegos. Este método no realiza
     * ningún cambio en el dataStore y no revisa que los datos móbiles
     * estén encendidos.
     * */
    private fun stopFirewallService() {
        runCatching {
            val intent = Intent(context, FirewallService::class.java).apply {
                action = FirewallService.ACTION_STOP_FIREWALL_SERVICE
            }

            context.startService(intent)
        }
    }

    /**
     * Establece en el dataStore que el cortafuegos está abilitado.
     *
     * @param enabled
     * */
    private suspend fun establishFirewallEnabled(enabled: Boolean) {
        withContext(Dispatchers.IO) {
            dataStore.edit {
                it[PreferencesKeys.ENABLED_FIREWALL] = enabled
            }
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
        return withContext(Dispatchers.IO) {
            dataStore.data
                .firstOrNull()?.get(PreferencesKeys.ENABLED_FIREWALL) == true
        }
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
        return dataStore.data
            .map {
                return@map it[PreferencesKeys.ENABLED_FIREWALL] ?: false
            }
    }

    private fun notify(@StringRes titleRes: Int, @StringRes msgRes: Int) {
        notify(context.getString(titleRes), context.getString(msgRes))
    }

    private fun notify(title: String, message: String) {
        val notification = NotificationCompat.Builder(context, NotificationHelper.ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_main_notification)
            .setContentTitle(title)
            .setContentText(message)
            .build()

        NotificationManagerCompat.from(context)
            .notify(NotificationHelper.ALERT_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "FirewallHelper"
    }
}