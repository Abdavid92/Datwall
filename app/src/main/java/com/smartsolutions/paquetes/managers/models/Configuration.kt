package com.smartsolutions.paquetes.managers.models

/**
 * Una configuración de la aplicación.
 * */
data class Configuration(
    /**
     * Indica si es obligatoria.
     * */
    val require: Boolean,
    /**
     * Qué se configura.
     * */
    val step: ConfigurationStep,
    /**
     * Indica si está completada o no. Osea si está configuración se completo
     * correctamente o si se dañó o borró.
     * */
    val completed: suspend () -> Boolean
)