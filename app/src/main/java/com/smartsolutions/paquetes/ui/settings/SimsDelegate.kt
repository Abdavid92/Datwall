package com.smartsolutions.paquetes.ui.settings

import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager2
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class SimsDelegate(
    private val context: Context,
    private val simManager: ISimManager2,
    private val permissionsManager: IPermissionsManager,
    private val scope: CoroutineScope
) {

    private val _sims = MutableLiveData<List<Sim>>()

    fun getSims(
        fragment: AbstractSettingsFragment,
        fragmentManager: FragmentManager
    ): LiveData<List<Sim>> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionsManager
                .findPermission(IPermissionsManager.CALL_CODE)?.let { permission ->
                    if (!permission.checkPermission(permission, context)) {

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

    private fun fillSims() {
        scope.launch {
            simManager.flowInstalledSims().collect {
                _sims.postValue(it)
            }
        }
    }
}