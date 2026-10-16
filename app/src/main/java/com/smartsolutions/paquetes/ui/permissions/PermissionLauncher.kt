package com.smartsolutions.paquetes.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.managers.models.PermissionKind

/**
 * Pide un permiso según su [PermissionKind]. El resultado llega a `onRequestPermissionsResult`
 * (permisos del sistema) o a `onActivityResult` (ajustes y VPN) de quien lo pide, con el
 * `requestCode` del permiso.
 * */
object PermissionLauncher {

    fun request(permission: Permission, activity: Activity) {
        val intent = settingsIntent(permission, activity)

        if (intent != null)
            activity.startActivityForResult(intent, permission.requestCode)
        else if (permission.kind == PermissionKind.RUNTIME)
            activity.requestPermissions(permission.keys, permission.requestCode)
    }

    fun request(permission: Permission, fragment: Fragment) {
        val intent = settingsIntent(permission, fragment.requireContext())

        if (intent != null)
            fragment.startActivityForResult(intent, permission.requestCode)
        else if (permission.kind == PermissionKind.RUNTIME)
            fragment.requestPermissions(permission.keys, permission.requestCode)
    }

    /**
     * Pantalla del sistema donde se concede un permiso especial; null para los permisos con
     * diálogo propio, o si la VPN ya está concedida.
     * */
    private fun settingsIntent(permission: Permission, context: Context): Intent? = when (permission.kind) {
        PermissionKind.RUNTIME -> null
        PermissionKind.USAGE_ACCESS -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        PermissionKind.OVERLAY -> Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                data = Uri.parse("package:${context.packageName}")
        }
        PermissionKind.VPN -> VpnService.prepare(context)
    }
}
