package com.smartsolutions.paquetes.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    permissionsManager: IPermissionsManager,
    private val kernel: DatwallKernel
) : ViewModel() {

    val permissions = permissionsManager.getDeniedPermissions(false)

    fun finish(permissionsActivity: PermissionsActivity) {
        kernel.main(permissionsActivity)
    }
}