package com.smartsolutions.paquetes.helpers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Obtiene las configuraciones de la versión anterior de la aplicación.
 * */
class LegacyConfigurationHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val preferences = context
        .getSharedPreferences("data_mis_datos", Context.MODE_PRIVATE)

    /**
     * Indica si ya la versión anterior de la aplicación ha sido comprada.
     * */
    fun isPurchased(): Boolean {
        return preferences.getBoolean("l_p_f", false)
    }

    /**
     * Obtiene todas los nombres de paquetes de las aplicaciones permitidas de la
     * versión anterior.
     *
     * @return [List] con los nombres de paquetes de las aplicaciones
     * permitidas por el cortafuegos.
     * */
    fun getLegacyRules(): List<String> {
        val db = context.openOrCreateDatabase("rules.db", Context.MODE_PRIVATE, null)

        val result = mutableListOf<String>()

        try {
            val cursor = db.query(
                "apps",
                arrayOf("package_name"),
                "data_access = ?",
                arrayOf("1"),
                null,
                null,
                null
            )

            if (cursor.moveToFirst()) {
                var packageName = cursor.getString(cursor.getColumnIndex("package_name"))

                result.add(packageName)

                while (cursor.moveToNext()) {
                    packageName = cursor.getString(cursor.getColumnIndex("package_name"))

                    result.add(packageName)
                }
            }
            cursor.close()
        } catch (e: Exception) {

        }

        return result
    }

    /**
     * Establece en el SharedPreferences que la configuración
     * ya fué restaurada.
     * */
    fun setConfigurationRestored() {
        preferences.edit()
            .putBoolean(DB_CONFIGURATION_RESTORED, true)
            .apply()
    }

    /**
     * Indica si la configuración de la base de datos
     * ya fué restaurada.
     * */
    fun isConfigurationRestored(): Boolean {
        return preferences.getBoolean(DB_CONFIGURATION_RESTORED, false)
    }

    /**
     * Establece en el dataStore la configuración del cortafuegos de la versión anterior.
     * */
    fun setFirewallLegacyConfiguration() {
        val preferences = context.getSharedPreferences(
            "com.smartsolutions.paquetes_preferences",
            Context.MODE_PRIVATE
        )

        launch {
            context.dataStore.edit {
                it[PreferencesKeys.ENABLED_FIREWALL] = preferences
                    .getBoolean("firewall_running", false)
            }
        }
    }

    /**
     * Establece en el dataStore la configuración de la burbuja
     * flotante de la versión anterior.
     * */
    fun setBubbleFloatingLegacyConfiguration() {
        val preferences = context.getSharedPreferences(
            "com.smartsolutions.paquetes_preferences",
            Context.MODE_PRIVATE
        )

        launch {
            context.dataStore.edit {
                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = preferences
                    .getBoolean("widget_floating", false)
            }
        }
    }

    companion object {
        const val DB_CONFIGURATION_RESTORED = "db_configuration_restored"
    }
}