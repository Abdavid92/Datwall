package com.smartsolutions.paquetes.repositories

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.SpecialApp

abstract class BaseAppRepository(
    context: Context,
    gson: Gson
) : IAppRepository {

    protected val packageManager: PackageManager = context.packageManager

    private val specialApps: Array<SpecialApp>
    private val nationalApps: Array<String>

    init {
        var json = context.resources.openRawResource(R.raw.national_apps)
            .bufferedReader()
            .readText()

        nationalApps = gson.fromJson(json, Array<String>::class.java)

        json = context.resources.openRawResource(R.raw.special_apps)
            .bufferedReader()
            .readText()

        val type = object : TypeToken<Array<SpecialApp>>() {}.type
        specialApps = gson.fromJson(json, type)
    }

    override fun fillNewApp(app: App, info: PackageInfo) {
        app.packageName = info.packageName
        app.ask = true
        app.executable = isExecutable(info)
        app.foregroundAccess = false
        app.internet = hasInternet(info.packageName)
        app.tempAccess = false
        app.version = info.versionName
        app.access = false
        app.national = isNational(info.packageName)
        app.name = info.applicationInfo.name
        app.uid = info.applicationInfo.uid

        getSpecialApp(info.packageName)?.let {
            app.access = it.access
            app.allowAnnotations = it.allowAnnotations
            app.blockedAnnotations = it.blockedAnnotations
        }
    }

    override fun fillApp(app: App, info: PackageInfo) {
        app.version = info.versionName
        app.name = info.applicationInfo.name
        app.internet = hasInternet(app.packageName)
        getSpecialApp(app.packageName)?.let {
            app.allowAnnotations = it.allowAnnotations
            app.blockedAnnotations = it.blockedAnnotations
        }
    }

    private fun getSpecialApp(packageName: String): SpecialApp? {
        return specialApps.firstOrNull { it.packageName == packageName }
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