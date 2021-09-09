package com.smartsolutions.paquetes.managers

import android.os.Build
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import javax.inject.Inject
import javax.inject.Provider

class ConfigurationManager @Inject constructor(
    private val simConfigurationFragment: Provider<SimsConfigurationFragment>,
    private val packagesConfigurationFragment: Provider<PackagesConfigurationFragment>,
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

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
            Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP &&
            simManager.isSeveralSimsInstalled()) {
            list.add(
                Configuration(
                    true,
                    simConfigurationFragment,
                    {
                        try {
                            simManager.getDefaultSim(SimDelegate.SimType.DATA)
                            simManager.getDefaultSim(SimDelegate.SimType.VOICE)

                            return@Configuration true
                        } catch (e: IllegalStateException) {
                            return@Configuration false
                        }
                    }
                )
            )
        }
        list.add(
            Configuration(
                true,
                packagesConfigurationFragment,
                {
                    return@Configuration dataPackageManager.isConfiguredDataPackages()
                }
            )
        )

        return list.toTypedArray()
    }
}