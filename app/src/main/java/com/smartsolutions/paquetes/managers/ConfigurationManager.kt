package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.ui.activation.ApplicationStatusFragment
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.lang.mutable.Mutable
import javax.inject.Inject
import javax.inject.Provider

class ConfigurationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val simManager: ISimManager,
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


        if (addSimConfigurationFragment()) {
            list.add(
                Configuration(
                    true,
                    SimsConfigurationFragment::class.java
                ) {
                    try {
                        val voice = simManager.getDefaultSim(SimDelegate.SimType.VOICE)
                        val data = simManager.getDefaultSim(SimDelegate.SimType.DATA)

                        return@Configuration voice != null && data != null
                    } catch (e: IllegalStateException) {
                        return@Configuration false
                    }
                }
            )
        }
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


    private suspend fun addSimConfigurationFragment(): Boolean{
        if (simManager.isSeveralSimsInstalled()){
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N &&
                Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP ||
                simManager.isBrokenDualSim()){
               return true
            }
        }

        return false
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