package com.smartsolutions.paquetes.ui.settings

import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.material.snackbar.Snackbar
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.SimsHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.permissions.StartAccessibilityServiceFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.Exception

@HiltViewModel
class PackagesConfigurationViewModel @Inject constructor(
    private val simManager: ISimManager2,
    private val simsHelper: SimsHelper,
    private val dataPackageManager: IDataPackageManager
) : ViewModel() {


    fun geInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData()
    }

    fun invokeOnDefaultSim(
        sim: Sim,
        simType: SimDelegate.SimType,
        fragmentManager: FragmentManager,
        onDefault: () -> Unit
    ) {
        viewModelScope.launch {
            simsHelper.invokeOnDefault(sim, simType, fragmentManager, onDefault)
        }
    }

    fun configureAutomaticPackages(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ) {
        viewModelScope.launch {
            try {
                dataPackageManager.configureDataPackages()
            } catch (e: Exception) {
                if (e is USSDRequestException)
                    handleException(e, fragment, fragmentManager)
            }
        }
    }

    fun configureManualPackages(@Networks network: String, sim: Sim) {
        viewModelScope.launch {
            dataPackageManager.setDataPackagesManualConfiguration(network, sim)
        }
    }


    private fun handleException(
        e: USSDRequestException,
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ) {
        when (e.errorCode) {
            USSDHelper.DENIED_CALL_PERMISSION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    SinglePermissionFragment.newInstance(
                        IPermissionsManager.CALL_CODE,
                        object : SinglePermissionFragment.SinglePermissionCallback {
                            override fun onGranted() {
                                showSnackbar(
                                    fragment,
                                    fragmentManager,
                                    "Permiso concedido"
                                )
                            }

                            override fun onDenied() {
                                fragment.complete()
                            }

                        }
                    ).show(fragmentManager, null)
            }
            USSDHelper.TELEPHONY_SERVICE_UNAVAILABLE, USSDHelper.USSD_CODE_FAILED -> {
                showSnackbar(
                    fragment,
                    fragmentManager,
                    "Falló el código ussd"
                )
            }
            USSDHelper.ACCESSIBILITY_SERVICE_UNAVAILABLE -> {
                StartAccessibilityServiceFragment.newInstance(
                    object : SinglePermissionFragment.SinglePermissionCallback {
                        override fun onGranted() {
                            showSnackbar(
                                fragment,
                                fragmentManager,
                                "Servicio de Accesibilidad Encendido"
                            )
                        }

                        override fun onDenied() {
                            fragment.complete()
                        }

                    }
                ).show(fragmentManager, null)
            }
            USSDHelper.CONNECTION_TIMEOUT -> {
                showSnackbar(
                    fragment,
                    fragmentManager,
                    "Se demoró demasiado en responder"
                )
            }
        }
    }

    private fun showSnackbar(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager,
        msg: String
    ) {
        fragment.view?.let {
            Snackbar.make(
                it,
                msg,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.btn_retry) {
                configureAutomaticPackages(fragment, fragmentManager)
            }.show()
        }
    }

    fun isDefaultSim(sim: Sim, type: SimDelegate.SimType): Boolean? {
        return runBlocking {
            return@runBlocking simManager.isSimDefaultBoth(type, sim)
        }
    }

}