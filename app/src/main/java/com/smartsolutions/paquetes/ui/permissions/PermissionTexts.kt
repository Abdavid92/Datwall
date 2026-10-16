package com.smartsolutions.paquetes.ui.permissions

import androidx.annotation.StringRes
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Permission

/**
 * Nombre y descripción de cada permiso, por su código.
 * */
object PermissionTexts {

    @StringRes
    fun name(permission: Permission): Int = when (permission.requestCode) {
        IPermissionsManager.USAGE_ACCESS_CODE -> R.string.usage_access_permission
        IPermissionsManager.CALL_CODE -> R.string.call_permission
        IPermissionsManager.DRAW_OVERLAYS_CODE -> R.string.draw_overlays_permission
        IPermissionsManager.NOTIFICATIONS_CODE -> R.string.notifications_permission
        IPermissionsManager.VPN_CODE -> R.string.vpn_permission
        else -> throw IllegalArgumentException("Permiso sin textos: ${permission.requestCode}")
    }

    @StringRes
    fun description(permission: Permission): Int = when (permission.requestCode) {
        IPermissionsManager.USAGE_ACCESS_CODE -> R.string.usage_access_permission_description
        IPermissionsManager.CALL_CODE -> R.string.call_permission_description
        IPermissionsManager.DRAW_OVERLAYS_CODE -> R.string.draw_overlays_permission_description
        IPermissionsManager.NOTIFICATIONS_CODE -> R.string.notifications_permission_description
        IPermissionsManager.VPN_CODE -> R.string.vpn_permission_description
        else -> throw IllegalArgumentException("Permiso sin textos: ${permission.requestCode}")
    }
}
