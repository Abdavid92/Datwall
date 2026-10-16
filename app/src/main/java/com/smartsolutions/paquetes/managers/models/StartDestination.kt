package com.smartsolutions.paquetes.managers.models

/**
 * Parte de la app a la que debe ir el usuario al abrirla. Lo decide el arranque
 * (`DatwallKernel.main`); la UI elige la pantalla de cada destino.
 * */
sealed interface StartDestination {

    /**
     * Explicación de datos y permisos del primer uso.
     * */
    data object FirstUse : StartDestination

    /**
     * Configuración pendiente, empezando por [step].
     * */
    data class Setup(val step: ConfigurationStep) : StartDestination

    /**
     * Pantalla principal.
     * */
    data object Main : StartDestination
}
