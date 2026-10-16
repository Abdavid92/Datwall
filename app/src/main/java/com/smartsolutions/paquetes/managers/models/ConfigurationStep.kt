package com.smartsolutions.paquetes.managers.models

/**
 * Paso de la configuración inicial. La UI decide qué pantalla muestra cada uno.
 * */
enum class ConfigurationStep {
    /**
     * Líneas predeterminadas para datos y llamadas.
     * */
    SIMS,

    /**
     * Si cada línea puede comprar planes.
     * */
    PACKAGES
}
