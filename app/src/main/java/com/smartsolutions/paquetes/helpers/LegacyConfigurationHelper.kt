package com.smartsolutions.paquetes.helpers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Obtiene las configuraciones de la versión anterior de la aplicación.
 * */
class LegacyConfigurationHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

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
    @Deprecated("Se eliminara en próximas versiones")
    fun getLegacyRules(): List<String> {
        val db = context.openOrCreateDatabase("rules.db", Context.MODE_PRIVATE, null)

        val cursor = db.query(
            "apps",
            arrayOf("package_name"),
            "data_access = ?",
            arrayOf("1"),
            null,
            null,
            null
        )

        val result = mutableListOf<String>()

        if (cursor.moveToFirst()) {
            var packageName = cursor.getString(cursor.getColumnIndex("package_name"))

            result.add(packageName)

            while (cursor.moveToNext()) {
                packageName = cursor.getString(cursor.getColumnIndex("package_name"))

                result.add(packageName)
            }
        }
        cursor.close()

        return result
    }

    /**
     * Establece en el SharedPreferences que la configuración
     * ya fué restaurada.
     * */
    @Deprecated("Se eliminará en próximas versiones")
    fun setConfigurationRestored() {
        preferences.edit()
            .putBoolean(DB_CONFIGURATION_RESTORED, true)
            .apply()
    }

    /**
     * Indica si la configuración de la base de datos
     * ya fué restaurada.
     * */
    @Deprecated("Se eliminará en próximas versiones")
    fun isConfigurationRestored(): Boolean {
        return preferences.getBoolean(DB_CONFIGURATION_RESTORED, false)
    }

    /**
     * Establece en el dataStore la configuración del cortafuegos de la versión anterior.
     * */
    @Deprecated("Se eliminará en próximas versiones")
    fun setFirewallLegacyConfiguration() {
        val preferences = context.getSharedPreferences(
            "com.smartsolutions.paquetes_preferences",
            Context.MODE_PRIVATE
        )

        GlobalScope.launch {
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
    @Deprecated("Se eliminará en próximas versiones")
    fun setBubbleFloatingLegacyConfiguration() {
        val preferences = context.getSharedPreferences(
            "com.smartsolutions.paquetes_preferences",
            Context.MODE_PRIVATE
        )

        GlobalScope.launch {
            context.dataStore.edit {
                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = preferences
                    .getBoolean("widget_floating", false)
            }
        }
    }

    companion object {

        @Deprecated("Se eliminará en próximas versiones")
        const val DB_CONFIGURATION_RESTORED = "db_configuration_restored"
    }
}