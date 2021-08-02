package com.smartsolutions.paquetes.ui.settings

import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.exceptions.USSDRequestException
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
import kotlin.reflect.KClass

@HiltViewModel
class PackagesConfigurationViewModel @Inject constructor(
    private val dataPackageManager: IDataPackageManager,
    private val simManager: ISimManager
) : ViewModel() {

    private val _configurationResult = MutableLiveData<Result<Sim>>()
    val configurationResult: LiveData<Result<Sim>>
        get() = _configurationResult

    fun configureDataPackages(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dataPackageManager.configureDataPackages()

                _configurationResult.postValue(Result.Success(simManager.getDefaultVoiceSim()))
            } catch (e: USSDRequestException) {
                _configurationResult.postValue(Result.Failure(e))

                withContext(Dispatchers.Main) {
                    handleException(e, fragment, fragmentManager)
                }
            }
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

                            }

                            override fun onDenied() {
                                fragment.complete()
                            }

                        }
                    ).show(fragmentManager, null)
            }
            USSDHelper.TELEPHONY_SERVICE_UNAVAILABLE -> {

            }
            USSDHelper.USSD_CODE_FAILED -> {

            }
            USSDHelper.ACCESSIBILITY_SERVICE_UNAVAILABLE -> {
                StartAccessibilityServiceFragment.newInstance(
                    object : SinglePermissionFragment.SinglePermissionCallback {
                        override fun onGranted() {

                        }

                        override fun onDenied() {
                            fragment.complete()
                        }

                    }
                ).show(fragmentManager, null)
            }
            USSDHelper.CONNECTION_TIMEOUT -> {

            }
        }
    }

    fun setManualConfiguration(@Networks network: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dataPackageManager.setDataPackagesManualConfiguration(network)

            _configurationResult.postValue(Result.Success(simManager.getDefaultVoiceSim()))
        }
    }
}