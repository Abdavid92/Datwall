package com.smartsolutions.paquetes.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    application: Application,
    permissionsManager: IPermissionsManager
) : AndroidViewModel(application) {

    val permissions = permissionsManager.getDeniedPermissions(false)
}