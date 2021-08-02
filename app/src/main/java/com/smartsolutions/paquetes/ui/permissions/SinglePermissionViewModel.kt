package com.smartsolutions.paquetes.ui.permissions

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SinglePermissionViewModel @Inject constructor(
    private val permissionManager: IPermissionsManager
) : ViewModel() {

    var callback: SinglePermissionFragment.SinglePermissionCallback? = null

    lateinit var permission: Permission

    var granted = false

    fun initPermission(requestCode: Int?) {
        permission = permissionManager.findPermission(requestCode ?: -1)
            ?: throw IllegalArgumentException()
    }
}