package com.smartsolutions.paquetes.managers

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

open class PermissionsManager @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

     protected open val permissions = listOf(
        Permission(
            context.getString(R.string.usage_access_permission),
            emptyArray(),
            context.getString(R.string.usage_access_permission_description),
            Permission.Category.Optional,
            USAGE_ACCESS_CODE,
            checkPermission = { context ->
                val applicationInfo = context.packageManager
                    .getApplicationInfo(context.packageName, 0)

                val appOpsManager = ContextCompat
                    .getSystemService(context, AppOpsManager::class.java) ?: return@Permission false

                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    appOpsManager.unsafeCheckOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid,
                        applicationInfo.packageName
                    )
                else
                    appOpsManager.checkOpNoThrow(
                        AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid,
                        applicationInfo.packageName
                    )

                mode == AppOpsManager.MODE_ALLOWED
            },
            requestPermissionActivity = { activity ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                activity.startActivityForResult(intent, requestCode)
            },
            requestPermissionFragment = { fragment ->
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                fragment.startActivityForResult(intent, requestCode)
            }
        ),
        Permission(
            context.getString(R.string.vpn_permission),
            emptyArray(),
            context.getString(R.string.vpn_permission_description),
            Permission.Category.Optional,
            VPN_CODE,
            checkPermission = { context ->
                intent = VpnService.prepare(context)
                intent == null
            },
            requestPermissionActivity = { activity ->
                if (intent != null) {
                    activity.startActivityForResult(intent, requestCode)
                } else {
                    VpnService.prepare(activity)?.let {
                        activity.startActivityForResult(it, requestCode)
                    }
                }
            },
            requestPermissionFragment = { fragment ->
                if (intent != null) {
                    fragment.startActivityForResult(intent, requestCode)
                } else {
                    fragment.context?.let { context ->
                        VpnService.prepare(context)?.let { serviceIntent ->
                            fragment.startActivityForResult(serviceIntent, requestCode)
                        }
                    }
                }
            }
        )
    )

    fun findPermission(requestCode: Int) =
        permissions.firstOrNull { it.requestCode == requestCode }

    fun findPermissions(requestCodes: IntArray): List<Permission> {
        val list = mutableListOf<Permission>()

        requestCodes.forEach { code ->
            permissions.firstOrNull { it.requestCode == code }?.let {
                list.add(it)
            }
        }
        return list
    }

    fun getDeniedPermissions(onlyRequired: Boolean = true) =
        permissions.filter {
            (if (onlyRequired)
                it.category == Permission.Category.Required
            else
                true) && !it.checkPermission(it, context)
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