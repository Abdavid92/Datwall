package com.smartsolutions.paquetes.ui.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.ui.permissions.SinglePermissionFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.settingsDataStore

/**
 * Delegado de la ui que se encarga de pedir las lineas instaladas al
 * [ISimManager] pero antes revisa que esté consedido el permiso de llamada.
 * */
class SimsDelegate(
    private val context: Context,
    private val simManager: ISimManager,
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
                                // Sin permiso no se pueden leer las líneas: se deja la
                                // configuración para cuando lo conceda.
                                scope.launch {
                                    context.settingsDataStore.edit {
                                        it[PreferencesKeys.SIM_SETUP_SKIPPED] = true
                                    }
                                    withContext(Dispatchers.Main) {
                                        fragment.skipSetup()
                                    }
                                }
                            }
                        }
                    ).show(fragment.childFragmentManager, null)
                } else {
                    fillSims()
                }
            }

        return _sims
    }

    private fun fillSims() {
        scope.launch {
            _sims.postValue(simManager.getInstalledSims())
        }
    }
}