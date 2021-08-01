package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.managers.models.Configuration

/**
 * Administrador de configuraciones.
 * */
interface IConfigurationManager {

    /**
     * Obtiene todas la configuraciones.
     *
     * @return [Array]
     * */
    suspend fun getConfigurations(): Array<Configuration>

    /**
     * Obtiene todas la configuraciones obligatorias.
     *
     * @return [Array]
     * */
    suspend fun getRequiredConfigurations(): Array<Configuration>

    /**
     * Obtiene todas la configuraciones incompletas.
     *
     * @param onlyRequires - Indica si se deben obtener solo las obligatorias.
     *
     * @return [Array] con las configuraciones incompletas.
     * */
    suspend fun getUncompletedConfigurations(onlyRequires: Boolean = false): Array<Configuration>
}