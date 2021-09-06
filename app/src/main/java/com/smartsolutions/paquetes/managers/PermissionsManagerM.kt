package com.smartsolutions.paquetes.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.BATTERY_OPTIMIZATION_CODE
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.CALL_CODE
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.DRAW_OVERLAYS_CODE
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager.Companion.SMS_CODE
import com.smartsolutions.paquetes.managers.models.Permission
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.M)
class PermissionsManagerM @Inject constructor(
    @ApplicationContext
    private val context: Context
) : PermissionsManager(context) {

    @SuppressLint("BatteryLife")
    @Suppress("DEPRECATION")
    override val permissions = listOf(
        Permission(
            context.getString(R.string.call_permission),
            arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
            context.getString(R.string.call_permission_description),
            Permission.Category.Required,
            CALL_CODE
        ),
        Permission(
            context.getString(R.string.sms_permission),
            arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
            context.getString(R.string.sms_permission_description),
            Permission.Category.Required,
            SMS_CODE
        ),
        Permission(
            context.getString(R.string.draw_overlays_permission),
            emptyArray(),
            context.getString(R.string.draw_overlays_permission_description),
            Permission.Category.Optional,
            DRAW_OVERLAYS_CODE,
            checkPermission = { context ->
                Settings.canDrawOverlays(context)
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
            context.getString(R.string.battery_optimization_permission),
            emptyArray(),
            context.getString(R.string.battery_optimization_permission_description),
            Permission.Category.Required,
            BATTERY_OPTIMIZATION_CODE,
            checkPermission = { context ->
                val powerManager = ContextCompat.getSystemService(context, PowerManager::class.java)
                    ?: throw NullPointerException()

                powerManager.isIgnoringBatteryOptimizations(context.packageName)
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
        *super.permissions.toTypedArray()
    )
}