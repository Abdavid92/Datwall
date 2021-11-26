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

/**
 * Delegado de la ui que se encarga de pedir las lineas instaladas al
 * [ISimManager2] pero antes revisa que esté consedido el permiso de llamada.
 * */
class SimsDelegate(
    private val context: Context,
    private val simManager: ISimManager2,
    private val permissionsManager: IPermissionsManager,
    private val scope: CoroutineScope
) {

    private val _sims = MutableLiveData<List<Sim>>()

    /**
     * Obtiene las lineas instaladas pero antes revisa que el permiso
     * de llamadas esté concedido. De no ser así se pide el permiso y en caso
     * de que no se conceda se llama al método complete del fragmento dado.
     *
     * @param fragment
     *
     * @return [LiveData]
     * */
    fun getSims(
        fragment: AbstractSettingsFragment
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
                        ).show(fragment.childFragmentManager, null)
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
            _sims.postValue(simManager.getInstalledSims())
        }
    }
}