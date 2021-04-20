package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.services.FirewallService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Implementación de la interfaz IChangeNetworkHelper.
 * Esta clase está ubicada en el paquete superior para poder acceder a los servicios
 * de la aplicación y encenderlos o apagarlos.
 * */
class ChangeNetworkHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
): CoroutineScope, IChangeNetworkHelper {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun setDataMobileStateOn() {
        launch {
            context.dataStore.edit {
                it[PreferencesKeys.DATA_MOBILE_ON] = true
            }

            context.dataStore.data.map {
                if (it[PreferencesKeys.FIREWALL_ON] == true) {
                    val intent = Intent(context, FirewallService::class.java)

                    context.startService(intent)
                }
            }
        }
    }

    override fun setDataMobileStateOff() {
        launch {
            context.dataStore.edit {
                it[PreferencesKeys.DATA_MOBILE_ON] = false
            }

            context.dataStore.data.map {
                if (it[PreferencesKeys.FIREWALL_ON] == true) {
                    val intent = Intent(context, FirewallService::class.java)
                        .setAction(FirewallService.ACTION_STOP_FIREWALL_SERVICE)

                    context.startService(intent)
                }
            }
        }
    }
}