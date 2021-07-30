package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.managers.models.Configuration

interface IConfigurationManager {

    val configurations: Array<Configuration>

    val requiredConfigurations: Array<Configuration>

    suspend fun getIncompletedConfigurations(): Array<Configuration>
}