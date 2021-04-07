package com.smartsolutions.datwall

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore

/**
 * Contiene las claves de las preferencias.
 * */
object PreferencesKeys {
    val Context.datastore : DataStore<Preferences> by preferencesDataStore(name = "settings")

    /**
     * Indica si el cortafuegos debe ser encendido o no.
     * Esta clave debe ser de tipo Boolean.
     * */

    val FIREWALL_ON = booleanPreferencesKey("firewall_on")

    /**
     * Indica si el modo dinámico esta encencido o no.
     * Esta clave debe ser de tipo Boolean.
     * */
    val DYNAMIC_FIREWALL_ON = booleanPreferencesKey("dynamic_firewall_on")
}