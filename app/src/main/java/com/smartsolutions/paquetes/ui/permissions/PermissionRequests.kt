package com.smartsolutions.paquetes.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.PermissionRequestPolicy
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.managers.models.Permission

/**
 * Qué hacer al pulsar "Conceder" sobre [permission]. Los permisos especiales (acceso de uso,
 * superposición, VPN) se conceden en una pantalla del sistema y nunca quedan rechazados de
 * forma permanente.
 * */
suspend fun IPermissionsManager.actionFor(permission: Permission, activity: Activity): PermissionAction {
    val isRuntime = permission.keys.isNotEmpty()

    return PermissionRequestPolicy.decide(
        granted = permission.checkPermission(permission, activity),
        requestedBefore = isRuntime && wasRequested(permission.requestCode),
        canAskAgain = permission.keys.any {
            ActivityCompat.shouldShowRequestPermissionRationale(activity, it)
        }
    )
}

/**
 * Abre la pantalla inicial de la app descartando las demás. El arranque ya se reinició y esa pantalla
 * sigue su destino.
 * */
fun Activity.openStartScreen() {
    startActivity(
        Intent(this, SplashActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    )
}

/**
 * Abre la pantalla de ajustes de la app, donde el usuario puede conceder un permiso
 * rechazado de forma permanente.
 * */
fun Context.openAppSettings() {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}
