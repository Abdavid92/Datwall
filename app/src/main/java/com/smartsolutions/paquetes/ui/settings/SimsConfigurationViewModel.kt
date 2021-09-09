package com.smartsolutions.paquetes.ui.settings

import android.app.Application
import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SimsConfigurationViewModel @Inject constructor(
    application: Application,
    private val simManager: ISimManager,
    private val permissionsManager: IPermissionsManager
): AndroidViewModel(application) {

    private val _sims = MutableLiveData<List<Sim>>()

    fun getSims(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ): LiveData<List<Sim>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionsManager
                .findPermission(IPermissionsManager.CALL_CODE)?.let { permission ->
                    if (!permission.checkPermission(permission, getApplication())) {

                        SinglePermissionFragment.newInstance(
                            IPermissionsManager.CALL_CODE,
                            object : SinglePermissionFragment.SinglePermissionCallback {
                                override fun onGranted() {
                                    fillSims()
                                }

                                override fun onDenied() {
                                    fragment.complete()
                                }
                            }
                        ).show(fragmentManager, null)
                    } else {
                        fillSims()
                    }
                }
        } else {
            fillSims()
        }

        return _sims
    }

    fun saveChanges(defaultDataSim: Sim, defaultVoiceSim: Sim) {
        viewModelScope.launch(Dispatchers.IO) {
            simManager.setDefaultSim(SimDelegate.SimType.DATA, defaultDataSim)
            simManager.setDefaultSim(SimDelegate.SimType.VOICE, defaultVoiceSim)
        }
    }

    private fun fillSims() {
        viewModelScope.launch(Dispatchers.IO) {
            simManager.flowInstalledSims().collect {
                _sims.postValue(it)
            }
        }
    }
}