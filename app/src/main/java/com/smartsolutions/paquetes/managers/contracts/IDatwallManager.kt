package com.smartsolutions.paquetes.managers.contracts

interface IDatwallManager {

    /**
     * Verifica que el servicio de accesibilidad este encendido
     * y listo para funcionar.
     * */
    fun accessibilityServiceEnabled(): Boolean

    /**
     * Enciende el servicio de accesibilidad.
     * */
    suspend fun startAccessibilityService()
}