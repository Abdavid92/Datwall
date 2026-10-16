package com.smartsolutions.paquetes.managers

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.PreferencesKeys
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.settingsDataStore
import kotlinx.coroutines.flow.first
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.USAGE_ACCESS_CODE
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.VPN_CODE
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.managers.models.PermissionKind
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

open class PermissionsManager @Inject constructor(
    @ApplicationContext
    private val context: Context
) : IPermissionsManager {


     override val permissions = listOf(
         Permission(
             emptyArray(),
             USAGE_ACCESS_CODE,
             21,
             kind = PermissionKind.USAGE_ACCESS,
             atStartup = true,
             checkPermission = { context ->
                 val applicationInfo = context.packageManager
                     .getApplicationInfo(context.packageName, 0)

                 val appOpsManager = ContextCompat
                     .getSystemService(context, AppOpsManager::class.java) ?: return@Permission false

                 val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                     appOpsManager.unsafeCheckOpNoThrow(
                         AppOpsManager.OPSTR_GET_USAGE_STATS,
                         applicationInfo.uid,
                         applicationInfo.packageName
                     )
                 } else {
                     @Suppress("DEPRECATION")
                     appOpsManager.checkOpNoThrow(
                         AppOpsManager.OPSTR_GET_USAGE_STATS,
                         applicationInfo.uid,
                         applicationInfo.packageName
                     )
                 }

                 mode == AppOpsManager.MODE_ALLOWED
             }
         ),
         Permission(
             arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
             IPermissionsManager.CALL_CODE,
             23
         ),
         Permission(
             emptyArray(),
             IPermissionsManager.DRAW_OVERLAYS_CODE,
             23,
             kind = PermissionKind.OVERLAY,
             checkPermission = { context ->
                 Settings.canDrawOverlays(context)
             }
         ),
        Permission(
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            IPermissionsManager.NOTIFICATIONS_CODE,
            Build.VERSION_CODES.TIRAMISU,
            atStartup = true,
            checkPermission = { context ->
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

                !isNotificationPermissionRequired(Build.VERSION.SDK_INT, granted)
            }
        ),
        Permission(
            emptyArray(),
            VPN_CODE,
            21,
            kind = PermissionKind.VPN,
            checkPermission = { context ->
                VpnService.prepare(context) == null
            }
        )
    ).filter { Build.VERSION.SDK_INT >= it.minSdk }

    override fun findPermission(requestCode: Int) =
        permissions.firstOrNull { it.requestCode == requestCode }

    override fun isGranted(requestCode: Int): Boolean =
        findPermission(requestCode)?.let { it.checkPermission(it, context) } ?: true

    override fun findPermissions(requestCodes: IntArray): List<Permission> {
        val list = mutableListOf<Permission>()

        requestCodes.forEach { code ->
            permissions.firstOrNull { it.requestCode == code }?.let {
                list.add(it)
            }
        }
        return list
    }

    override fun getDeniedPermissions(onlyAtStartup: Boolean) =
        permissions.filter {
            (!onlyAtStartup || it.atStartup) && !it.checkPermission(it, context)
        }

    override suspend fun wasRequested(requestCode: Int): Boolean =
        context.settingsDataStore.data.first()[PreferencesKeys.REQUESTED_PERMISSIONS]
            ?.contains(requestCode.toString()) == true

    override suspend fun markRequested(requestCode: Int) {
        context.settingsDataStore.edit {
            it[PreferencesKeys.REQUESTED_PERMISSIONS] =
                (it[PreferencesKeys.REQUESTED_PERMISSIONS] ?: emptySet()) + requestCode.toString()
        }
    }

}