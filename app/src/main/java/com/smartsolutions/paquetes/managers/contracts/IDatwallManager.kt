package com.smartsolutions.paquetes.managers.contracts

interface IDatwallManager {

    /**
     * Verifica que el servicio de accesibilidad este encendido
     * y listo para funcionar.
     * */
    fun accessibilityServiceEnabled(): Boolean

    /**
     * Enciende el servicio de accesibilidad.
     *
     * @return true si se logró encender, vincular con el sistema
     * y está listo para funcionar.
     * */
    suspend fun startAccessibilityService(): Boolean
}