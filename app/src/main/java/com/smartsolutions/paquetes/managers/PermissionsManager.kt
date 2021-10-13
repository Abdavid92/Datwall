package com.smartsolutions.paquetes.managers

import android.Manifest
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.USAGE_ACCESS_CODE
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.VPN_CODE
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

open class PermissionsManager @Inject constructor(
    @ApplicationContext
    private val context: Context
) : IPermissionsManager {


     protected open val permissions = listOf(
         Permission(
             context.getString(R.string.usage_access_permission),
             emptyArray(),
             context.getString(R.string.usage_access_permission_description),
             Permission.Category.Required,
             USAGE_ACCESS_CODE,
             21,
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
             context.getString(R.string.call_permission),
             arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
             context.getString(R.string.call_permission_description),
             Permission.Category.Required,
             IPermissionsManager.CALL_CODE,
             23
         ),
         Permission(
             context.getString(R.string.sms_permission),
             arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
             context.getString(R.string.sms_permission_description),
             Permission.Category.Required,
             IPermissionsManager.SMS_CODE,
             23
         ),
         Permission(
             context.getString(R.string.battery_optimization_permission),
             emptyArray(),
             context.getString(R.string.battery_optimization_permission_description),
             Permission.Category.Required,
             IPermissionsManager.BATTERY_OPTIMIZATION_CODE,
             23,
             checkPermission = { context ->
                 val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
                     ?: throw NullPointerException()

                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                     powerManager.isIgnoringBatteryOptimizations(context.packageName)
                 } else {
                    true
                 }
             },
             requestPermissionActivity = { activity ->
                 activity.startActivityForResult(
                     Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                         .setData(Uri.parse("package:${activity.packageName}")),
                     requestCode
                 )
             },
             requestPermissionFragment = { fragment ->
                 fragment.activity?.let {
                     requestPermissionActivity(this, it)
                 }
             }
         ),
         Permission(
             context.getString(R.string.draw_overlays_permission),
             emptyArray(),
             context.getString(R.string.draw_overlays_permission_description),
             Permission.Category.Optional,
             IPermissionsManager.DRAW_OVERLAYS_CODE,
             23,
             checkPermission = { context ->
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                     Settings.canDrawOverlays(context)
                 } else {
                     true
                 }
             },
             requestPermissionActivity = { activity ->
                 val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)

                 if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                     intent.data = Uri.parse("package:${activity.packageName}")

                 activity.startActivityForResult(intent, requestCode)
             },
             requestPermissionFragment = { fragment ->
                 val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)

                 fragment.context?.let { context ->
                     if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R)
                         intent.data = Uri.parse("package:${context.packageName}")
                 }

                 fragment.startActivityForResult(intent, requestCode)
             }
         ),
        Permission(
            context.getString(R.string.vpn_permission),
            emptyArray(),
            context.getString(R.string.vpn_permission_description),
            Permission.Category.Optional,
            VPN_CODE,
            21,
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
    ).filter { Build.VERSION.SDK_INT >= it.minSdk }

    override fun findPermission(requestCode: Int) =
        permissions.firstOrNull { it.requestCode == requestCode }

    override fun findPermissions(requestCodes: IntArray): List<Permission> {
        val list = mutableListOf<Permission>()

        requestCodes.forEach { code ->
            permissions.firstOrNull { it.requestCode == code }?.let {
                list.add(it)
            }
        }
        return list
    }

    override fun getDeniedPermissions(onlyRequired: Boolean) =
        permissions.filter {
            (if (onlyRequired)
                it.category == Permission.Category.Required
            else
                true) && !it.checkPermission(it, context)
        }

}