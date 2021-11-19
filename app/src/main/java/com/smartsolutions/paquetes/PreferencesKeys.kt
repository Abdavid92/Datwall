package com.smartsolutions.paquetes

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.smartsolutions.paquetes.workers.SynchronizationWorker

/**
 * Contiene los ajustes de la aplicación.
 * */
val Context.settingsDataStore by preferencesDataStore(name = "settings")

/**
 * Contiene configuraciones de la ui
 * */
val Context.uiDataStore by preferencesDataStore(name = "settings_ui")

/**
 * Se usa para guardar datos necesarios en el backend.
 * */
val Context.internalDataStore by preferencesDataStore(name = "internal_settings")

/**
 * Se usa para guardar el estado de los workers
 * */
val Context.workersDataStore by preferencesDataStore(name = "workers_settings")

/**
 * Contiene las claves de las preferencias.
 * */
object PreferencesKeys {
    /**
     * Indica si no se pueden obtener las lineas predeterminadas del sistema y por ende se necesita configuración manual
     */
    val IS_DUAL_SIM_BROKEN = booleanPreferencesKey("is_dual_sim_broken")

    /**
     * Indica la hora en que se empezó a esperar la confirmación de la compra
     */
    val WAITING_PUCHASE = longPreferencesKey("waiting_purchase")

    /**
     * Licencia de la aplicación.
     * */
    val LICENSE = stringPreferencesKey("license")

    /**
     * Implementación de la notificación principal.
     * */
    val NOTIFICATION_CLASS = stringPreferencesKey("notification_class")

    /**
     * Indica si el tema de la notificación principal se debe guiar por
     * el tema de la aplicación o del sistema.
     * */
    val NOTIFICATION_STYLE_THEME_MODE_APP = booleanPreferencesKey("notification_style_theme_mode_app")

    /**
     * Indica si se deben descargar automáticamente las actualizaciones.
     * */
    val AUTO_DOWNLOAD_UPDATE = booleanPreferencesKey("auto_download_update")

    /**
     * Indica si se debe notificar cuando haya una actualización disponible.
     * */
    val ENABLED_NOTIFICATION_UPDATE = booleanPreferencesKey("enabled_notification_update")

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
     * Indica si el dispositotivo se puede conectar a la red LTE
     * */
    val ENABLED_LTE = booleanPreferencesKey("enabled_lte")

    /**
     * Identificador del dispositivo
     * */
    val DEVICE_ID = stringPreferencesKey("device_id")

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
     * Indica el modo de Actualización, si por Apklis o por nuestro servidor
     */
    val UPDATE_MODE = stringPreferencesKey("update_mode")

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

    /**
     * Coordenadas de la última posición de la burbuja en la pantalla
     */
    val BUBBLE_POSITION_X = intPreferencesKey("bubble_position_x")

    val BUBBLE_POSITION_Y = intPreferencesKey("bubble_position_y")

    /**
     * Filtro que organiza los userDataBytes que se muestran al usuario
     */
    val RESUME_FILTER = stringPreferencesKey("resume_filter")

    /**
     * Tema principal de la aplicación
     * */
    val APP_THEME = intPreferencesKey("app_theme")

    /**
     * Modo del tema (claro, oscuro, predeterminado)
     * */
    val THEME_MODE = intPreferencesKey("theme_mode")

    /**
     * Notificaciones secundarias
     * */
    val SHOW_SECONDARY_NOTIFICATIONS = booleanPreferencesKey("show_secondary_notifications")

    val INTERNATIONAL_NOTIFICATION = intPreferencesKey("international_notification")

    val INTERNATIONAL_LTE_NOTIFICATION = intPreferencesKey("international_lte_notification")

    val PROMO_BONUS_NOTIFICATION = intPreferencesKey("promo_bonus_notification")

    val NATIONAL_NOTIFICATION = intPreferencesKey("national_notification")

    val DAILY_BAG_NOTIFICATION = intPreferencesKey("daily_bag_notification")

    /**
     * Estado y Restricciones de la sincronización en segundo plano
     */
    val ENABLE_DATA_SYNCHRONIZATION = booleanPreferencesKey("enable_data_synchronization")

    /**
     * Indica si se debe usar la api moderna para ejecutar los códigos USSD o la antigua
     */
    val SYNCHRONIZATION_USSD_MODE_MODERN = booleanPreferencesKey("synchronization_ussd_mode_modern")

    /**
     * Indica si ya fueron sembrados los paquetes comprados anteriormente
     */
    val IS_SEED_OLD_PURCHASED_PACKAGES = booleanPreferencesKey("is_seed_old_purchased_packages")

    /**
     * Indica si la app se cerro previamente debido a una excepcion y por lo tanto el Kernel no debe iniciar sus servicios
     */
    val IS_THROWED = booleanPreferencesKey("is_throwed")

    /**
     * Indica que se debe forzar el modo de una sola linea.
     * */
    val FORCE_MODE_SINGLE_SIM = booleanPreferencesKey("force_mode_single_sim")

    /**
     * Busca una clave de preferencia por el nombre.
     *
     * @param key
     *
     * @return [Preferences.Key]
     * */
    @Suppress("UNCHECKED_CAST")
    fun <T> findPreferenceByKey(key: String): Preferences.Key<T>? {
        this.javaClass.declaredFields.forEach {

            if (it.type.name == Preferences.Key::class.java.name) {
                val preferenceKey = it.get(null) as Preferences.Key<T>

                if (preferenceKey.name == key)
                    return preferenceKey
            }
        }

        return null
    }
}