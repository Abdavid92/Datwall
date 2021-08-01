package com.smartsolutions.paquetes.managers.contracts

import android.Manifest
import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.managers.models.Permission

interface IPermissionsManager {

    fun findPermission(requestCode: Int): Permission?

    fun findPermissions(requestCodes: IntArray): List<Permission>

    fun getDeniedPermissions(onlyRequired: Boolean = true): List<Permission>

    @SuppressLint("NewApi")
    fun getRequestCode(permissionName: String): Int {
        return when (permissionName) {
            Manifest.permission.CALL_PHONE -> CALL_CODE
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS -> SMS_CODE
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION -> DRAW_OVERLAYS_CODE
            AppOpsManager.OPSTR_GET_USAGE_STATS -> USAGE_ACCESS_CODE
            else -> -1
        }
    }

    companion object {
        @RequiresApi(Build.VERSION_CODES.M)
        const val CALL_CODE = 34
        @RequiresApi(Build.VERSION_CODES.M)
        const val SMS_CODE = 22
        @RequiresApi(Build.VERSION_CODES.M)
        const val DRAW_OVERLAYS_CODE = 356
        const val VPN_CODE = 248
        const val USAGE_ACCESS_CODE = 854
    }
}