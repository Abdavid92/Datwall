package com.smartsolutions.paquetes.managers.models

import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import javax.inject.Provider

/**
 * Una configuración de la aplicación.
 * */
data class Configuration(
    /**
     * Indica si es obligatoria.
     * */
    val required: Boolean,
    /**
     * Fragmento que va a manejar esta configuración.
     * */
    val fragment: Provider<out AbstractSettingsFragment>,
    /**
     * Indica si está completada o no. Osea si está configuración se completo
     * correctamente o si se dañó o borró.
     * */
    val completed: suspend () -> Boolean
)