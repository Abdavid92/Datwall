package com.smartsolutions.paquetes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Contiene las claves de las preferencias.
 * */
object PreferencesKeys {

    /**
     * Indica si el cortafuegos debe ser encendido o no.
     * Esta clave es de tipo Boolean.
     * */
    val FIREWALL_ON = booleanPreferencesKey("firewall_on")

    /**
     * Indica si el modo dinámico esta encencido o no.
     * Esta clave es de tipo Boolean.
     * */
    val DYNAMIC_FIREWALL_ON = booleanPreferencesKey("dynamic_firewall_on")

    /**
     * Modo de compra de los paquertes de datos.
     * */
    val BUY_MODE = stringPreferencesKey("buy_mode")

    /**
     * Modo de sincronización de los datos.
     * */
    val SYNCHRONIZATION_MODE = stringPreferencesKey("synchronization_mode")
}