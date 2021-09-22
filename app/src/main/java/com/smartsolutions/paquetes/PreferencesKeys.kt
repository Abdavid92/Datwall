package com.smartsolutions.paquetes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore

val Context.dataStore : DataStore<Preferences> by preferencesDataStore(name = "settings")
val Context.uiDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings_ui")

/**
 * Contiene las claves de las preferencias.
 * */
object PreferencesKeys {

    /**
     * Indica si es ya la aplicación fué abierta anteriormente.
     * */
    //val APP_WAS_OPEN = booleanPreferencesKey("app_was_open")

    /**
     * Indica si el cortafuegos debe ser encendido o no.
     * Esta clave es de tipo Boolean.
     * */
    val ENABLED_FIREWALL = booleanPreferencesKey("enabled_firewall")

    /**
     * Indica si el modo dinámico esta encencido o no.
     * Esta clave es de tipo Boolean.
     * */
    val ENABLED_DYNAMIC_FIREWALL = booleanPreferencesKey("enabled_dynamic_firewall")

    /**
     * Modo de compra de los paquetes de datos.
     * */
    val BUY_MODE = stringPreferencesKey("buy_mode")

    /**
     * Modo de sincronización de los datos.
     * */
    val SYNCHRONIZATION_MODE = stringPreferencesKey("synchronization_mode")

    /**
     * Linea predeterminada para ejecutar la sincronización automática.
     * */
    val DEFAULT_SYNCHRONIZATION_SIM_ID = stringPreferencesKey("default_synchronization_sim_id")

    /**
     * Indica si el dispositotivo se puede conectar a la red LTE
     * */
    val ENABLED_LTE = booleanPreferencesKey("enabled_lte")

    /**
     * Identificador del dispositivo
     * */
    val DEVICE_ID = stringPreferencesKey("device_id")

    /**
     * Indica si se está esperando la confirmación de la compra.
     * */
    val WAITING_PURCHASED = booleanPreferencesKey("waiting_purchased")

    /**
     * DeviceApp obtenido del servidor.
     * */
    val DEVICE_APP = stringPreferencesKey("device_app")

    /**
     * ID único de la descarga que proporciona el DownloadManager.
     */
    val DOWNLOAD_UPDATE_ID = longPreferencesKey("download_update_id")

    /**
     * Determina si se descarga automaticamente las actualizaciones.
     */
    val AUTO_UPDATE = booleanPreferencesKey("auto_update")

    /**
     * Intervalo en el cual se verificará el estado de la app.
     */
    val INTERVAL_UPDATE_SYNCHRONIZATION = longPreferencesKey("interval_update_synchronization")

    /**
     * Indica si la burbuja flotante está abilitada.
     * */
    val ENABLED_BUBBLE_FLOATING = booleanPreferencesKey("enabled_bubble_floating")

    /**
     * Filtro que se usa para ordenar la lista de aplicaciones
     * */
    val APPS_FILTER = stringPreferencesKey("apps_filter")

    /**
     * Versión actual de la lista de paquetes de datos.
     * */
    val CURRENT_PACKAGES_VERSION = intPreferencesKey("current_packages_version")

    /**
     * Opcion de periodo de tiempo en el cual mostrar el consumo
     */
    val USAGE_PERIOD = intPreferencesKey("usage_option")

    /**
     * Filtro que organiza las apps en el fragmento de consumo
     */
    val USAGE_FILTER = stringPreferencesKey("usage_filter")

    /**
     * Tamaño de la burbuja
     */
    val BUBBLE_SIZE = stringPreferencesKey("bubble_size")

    /**
     * Transparencia de la burbuja flotante
     */
    val BUBBLE_TRANSPARENCY = floatPreferencesKey("bubble_transparency")

    /**
     * Indica si se muestra siempre la burbuja o solo si la app abierta tiene consumo
     */
    val BUBBLE_ALWAYS_SHOW = booleanPreferencesKey("bubble_always_show")
}