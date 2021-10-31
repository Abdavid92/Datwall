package com.smartsolutions.paquetes.ui.settings

import android.app.Application
import android.os.Build
import android.view.View
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.google.android.material.snackbar.Snackbar
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.serverApis.models.Result
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import com.smartsolutions.paquetes.ui.permissions.StartAccessibilityServiceFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class PackagesConfigurationViewModel2 @Inject constructor(
    application: Application,
    private val simManager: ISimManager,
    private val dataPackageManager: IDataPackageManager,
    permissionsManager: IPermissionsManager
) : AndroidViewModel(application) {

    private val delegate = SimsDelegate(
        getApplication(),
        simManager,
        permissionsManager,
        viewModelScope
    )

    private val _configurationResult = MutableLiveData<Result<Sim>>()
    val configurationResult: LiveData<Result<Sim>>
        get() = _configurationResult

    /**
     * Último tipo de red que se pudo resolver para la linea.
     * */
    @Networks
    var lastNetworkResult: String = Networks.NETWORK_NONE
        private set

    /**
     * Indica si hay varias sims instaladas.
     * */
    fun isSeveralSimsInstalled() = simManager.isSeveralSimsInstalled()

    fun getSims(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ): LiveData<List<Sim>> {
        return delegate.getSims(fragment, fragmentManager)
    }

    fun configureDataPackages(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ) {
        viewModelScope.launch {
            try {
                dataPackageManager.configureDataPackages()

                val defaultSim = simManager.getDefaultSim(SimDelegate.SimType.VOICE)
                lastNetworkResult = defaultSim.network

                _configurationResult.postValue(Result.Success(defaultSim))
            } catch (e: USSDRequestException) {
                _configurationResult.postValue(Result.Failure(e))

                withContext(Dispatchers.Main) {
                    handleException(e, fragment, fragmentManager)
                }
            }
        }
    }

    fun setManualConfiguration(@Networks network: String) {
        viewModelScope.launch {
            dataPackageManager.setDataPackagesManualConfiguration(network)

            val defaultSim = simManager.getDefaultSim(SimDelegate.SimType.VOICE)
            lastNetworkResult = defaultSim.network

            _configurationResult.postValue(Result.Success(defaultSim))
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
            USSDHelper.TELEPHONY_SERVICE_UNAVAILABLE -> {
                throw RuntimeException("Telephony service unavailable")
            }
            USSDHelper.USSD_CODE_FAILED -> {
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
                                "Pruebe de nuevo"
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
                    "Se agotó el tiempo de espera"
                )
            }
        }
    }

    private fun showSnackbar(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager,
        msg: String) {
        fragment.view?.let {
            Snackbar.make(
                it,
                msg,
                Snackbar.LENGTH_LONG
            ).setAction(R.string.btn_retry) {
                configureDataPackages(fragment, fragmentManager)
            }.show()
        }
    }
}