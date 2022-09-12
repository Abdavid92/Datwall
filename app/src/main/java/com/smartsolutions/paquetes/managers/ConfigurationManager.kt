package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import javax.inject.Inject

class ConfigurationManager @Inject constructor(
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager
) : IConfigurationManager {

    private var configurations: Array<Configuration>? = null

    override suspend fun getConfigurations(): Array<Configuration> {
        if (configurations == null)
            configurations = fillConfigurations()
        return configurations!!
    }

    override suspend fun getRequiredConfigurations(): Array<Configuration> {
        return getConfigurations().filter { it.require }.toTypedArray()
    }

    override suspend fun getUncompletedConfigurations(onlyRequires: Boolean): Array<Configuration> =
        if (onlyRequires)
            getRequiredConfigurations().filter { !it.completed.invoke() }.toTypedArray()
        else
            getConfigurations().filter { !it.completed.invoke() }.toTypedArray()

    private suspend fun fillConfigurations(): Array<Configuration> {
        val list = mutableListOf<Configuration>()

        list.add(
            Configuration(
                true,
                SimsConfigurationFragment::class.java
            ) {
                val defaultSimData = simManager.getDefaultSimBoth(SimType.DATA)
                val defaultSimVoice = simManager.getDefaultSimBoth(SimType.VOICE)

                return@Configuration defaultSimData != null && defaultSimVoice != null
            }
        )

        list.add(
            Configuration(
                true,
                PackagesConfigurationFragment::class.java
            ) {
                return@Configuration dataPackageManager.isConfiguredDataPackages()
            }
        )

        return list.toTypedArray()
    }
}