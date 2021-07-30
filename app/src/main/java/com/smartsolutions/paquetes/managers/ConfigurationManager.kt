package com.smartsolutions.paquetes.managers

import android.os.Build
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Provider

class ConfigurationManager @Inject constructor(
    private val simConfigurationFragment: Provider<SimsConfigurationFragment>,
    private val packagesConfigurationFragment: Provider<PackagesConfigurationFragment>,
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager
) : IConfigurationManager {

    override val configurations = runBlocking {
        fillConfigurations()
    }

    override val requiredConfigurations: Array<Configuration>
        get() = configurations.filter { it.required }.toTypedArray()

    override suspend fun getIncompletedConfigurations(): Array<Configuration> =
        configurations.filter { !it.completed.invoke() }.toTypedArray()

    private suspend fun fillConfigurations(): Array<Configuration> {
        val list = mutableListOf<Configuration>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
            Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP &&
            simManager.isInstalledSeveralSims()) {
            list.add(
                Configuration(
                    true,
                    simConfigurationFragment,
                    {
                        try {
                            simManager.getDefaultDataSim()
                            simManager.getDefaultVoiceSim()

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