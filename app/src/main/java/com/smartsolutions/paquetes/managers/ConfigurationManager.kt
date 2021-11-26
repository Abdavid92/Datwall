package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.ui.activation.ApplicationStatusFragment
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import javax.inject.Inject

class ConfigurationManager @Inject constructor(
    private val simManager: ISimManager2,
    private val dataPackageManager: IDataPackageManager,
    private val activationManager: IActivationManager
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

        if (!isRegisteredAndValid()) {
            list.add(
                Configuration(
                    true,
                    ApplicationStatusFragment::class.java
                ) {
                    activationManager.canWork().first
                }
            )
        }

        list.add(
            Configuration(
                true,
                SimsConfigurationFragment::class.java
            ) {
                val defaultSimData = simManager.getDefaultSimBoth(SimDelegate.SimType.DATA)
                val defaultSimVoice = simManager.getDefaultSimBoth(SimDelegate.SimType.VOICE)

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


    /**
     * Indica si la aplicación ya está registrada en el servidor y
     * no está obsoleta o descontinuada.
     * */
    private suspend fun isRegisteredAndValid(): Boolean {
        val status = activationManager.canWork().second

        return status != IActivationManager.ApplicationStatuses.Discontinued &&
                status != IActivationManager.ApplicationStatuses.Unknown &&
                status != IActivationManager.ApplicationStatuses.Deprecated &&
                status != IActivationManager.ApplicationStatuses.TooMuchOld
    }
}