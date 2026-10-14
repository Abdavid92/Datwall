package com.smartsolutions.paquetes.managers.contracts

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.provider.Settings
import com.smartsolutions.paquetes.managers.models.Permission

interface IPermissionsManager {

    fun findPermission(requestCode: Int): Permission?

    fun findPermissions(requestCodes: IntArray): List<Permission>

    /**
     * Permisos no concedidos. Con [onlyAtStartup] solo los que se piden en el primer uso.
     * */
    fun getDeniedPermissions(onlyAtStartup: Boolean = false): List<Permission>

    /**
     * Todos los permisos disponibles en esta versión de Android.
     * */
    val permissions: List<Permission>

    /**
     * Indica si la app pidió alguna vez el permiso.
     * */
    suspend fun wasRequested(requestCode: Int): Boolean

    /**
     * Registra que la app pidió el permiso.
     * */
    suspend fun markRequested(requestCode: Int)

    @SuppressLint("NewApi")
    fun getRequestCode(permissionName: String): Int {
        return when (permissionName) {
            Manifest.permission.CALL_PHONE -> CALL_CODE
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> DRAW_OVERLAYS_CODE
            AppOpsManager.OPSTR_GET_USAGE_STATS -> USAGE_ACCESS_CODE
            else -> -1
        }
    }

    companion object {
        const val CALL_CODE = 34
        const val DRAW_OVERLAYS_CODE = 356
        const val VPN_CODE = 248
        const val USAGE_ACCESS_CODE = 854
        const val NOTIFICATIONS_CODE = 517
        const val VPN_PERMISSION_KEY = "vpn_permissions_key"
    }
}