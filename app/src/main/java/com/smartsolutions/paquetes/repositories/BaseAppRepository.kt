package com.smartsolutions.paquetes.repositories

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.smartsolutions.paquetes.repositories.models.App

abstract class BaseAppRepository(
    protected val packageManager: PackageManager
) : IAppRepository {

    protected fun fillNewApp(app: App, info: PackageInfo) {
        app.packageName = info.packageName
        app.ask = true
        app.executable = isExecutable(info)
        app.foregroundAccess = false
        app.internet = hasInternet(info.packageName)
        app.tempAccess = false
        app.version = info.versionName
        app.access = false
        
    }

    private fun isExecutable(info: PackageInfo): Boolean {
        val intent = packageManager.getLaunchIntentForPackage(info.packageName)
        return intent != null
    }

    private fun isSystem(packageInfo: PackageInfo): Boolean {
        return packageInfo.applicationInfo.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun hasInternet(packageName: String): Boolean {
        return packageManager.checkPermission("android.permission.INTERNET", packageName) == PackageManager.PERMISSION_GRANTED
    }

    private fun isEnabled(info: PackageInfo): Boolean {
        val setting: Int = try {
            packageManager.getApplicationEnabledSetting(info.packageName)
        } catch (ex: IllegalArgumentException) {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }
        return if (setting == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT) info.applicationInfo.enabled else setting == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
    }
}