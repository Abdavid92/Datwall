package com.smartsolutions.paquetes.repositories

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.SpecialApp
import com.smartsolutions.paquetes.repositories.models.TrafficType

abstract class AbstractAppRepository(
    private val context: Context,
    gson: Gson
) : IAppRepository {

    private val packageManager: PackageManager = context.packageManager

    private val specialApps: Array<SpecialApp>
    private val specialGroups: Array<SpecialApp>
    private val nationalApps: Array<String>
    private val freeApps: Array<String>

    init {
        var json = context.resources.openRawResource(R.raw.national_apps)
            .bufferedReader()
            .readText()

        nationalApps = gson.fromJson(json, Array<String>::class.java)

        json = context.resources.openRawResource(R.raw.free_apps)
            .bufferedReader()
            .readText()

        freeApps = gson.fromJson(json, Array<String>::class.java)

        json = context.resources.openRawResource(R.raw.special_apps)
            .bufferedReader()
            .readText()

        val type = object : TypeToken<Array<SpecialApp>>() {}.type
        specialApps = gson.fromJson(json, type)

        json = context.resources.openRawResource(R.raw.app_groups)
            .bufferedReader()
            .readText()

        specialGroups = gson.fromJson(json, type)
    }

    protected fun fillAppGroup(appGroup: AppGroup): AppGroup {

        val specialGroup = getSpecialGroup(appGroup)

        if (specialGroup != null) {
            appGroup.apply {
                packageName = specialGroup.packageName
                name = specialGroup.name!!
                allowAnnotations = specialGroup.allowAnnotations
                blockedAnnotations = specialGroup.blockedAnnotations
            }
        } else {
            createGroupName(appGroup)
        }

        return appGroup
    }

    private fun createGroupName(appGroup: AppGroup) {
        appGroup.name = context.getString(R.string.generic_group_name, appGroup.uid)
    }

    private fun getSpecialGroup(appGroup: AppGroup): SpecialApp? {
        appGroup.forEach { app ->
            specialGroups.forEach { specialGroup ->
                if (app.packageName == specialGroup.packageName)
                    return specialGroup
            }
        }
        return null
    }

    override fun fillNewApp(app: App, info: PackageInfo) {
        app.packageName = info.packageName
        app.ask = true
        app.executable = isExecutable(info)
        app.system = isSystem(info)
        app.foregroundAccess = false
        app.internet = hasInternet(info.packageName)
        app.tempAccess = false
        app.version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        app.access = false
        app.trafficType = when {
            isFree(info.packageName) -> TrafficType.Free
            isNational(info.packageName) -> TrafficType.National
            else -> TrafficType.International
        }
        app.name = info.applicationInfo.loadLabel(packageManager).toString()
        app.uid = info.applicationInfo.uid

        getSpecialApp(info.packageName)?.let {
            app.access = it.access
            app.allowAnnotations = it.allowAnnotations
            app.blockedAnnotations = it.blockedAnnotations
        }
    }

    override fun fillApp(app: App, info: PackageInfo) {
        app.version = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode else info.versionCode.toLong()
        app.name = info.applicationInfo.loadLabel(packageManager).toString()
        app.uid = info.applicationInfo.uid
        app.internet = hasInternet(app.packageName)
        app.system = isSystem(info)
        getSpecialApp(app.packageName)?.let {
            app.allowAnnotations = it.allowAnnotations
            app.blockedAnnotations = it.blockedAnnotations
        }
    }

    private fun getSpecialApp(packageName: String): SpecialApp? {
        return specialApps.firstOrNull { it.packageName == packageName }
    }

    private fun isFree(packageName: String): Boolean {
        return freeApps.contains(packageName)
    }

    private fun isNational(packageName: String): Boolean {
        return nationalApps.contains(packageName)
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
}