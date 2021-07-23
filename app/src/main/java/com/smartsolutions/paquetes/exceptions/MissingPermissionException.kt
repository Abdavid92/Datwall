package com.smartsolutions.paquetes.exceptions

import android.app.Activity
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import java.io.Serializable

class MissingPermissionException: Exception, Serializable {

    val permission: Array<String>

    constructor(permission: String): super() {
        this.permission = arrayOf(permission)
    }

    constructor(permission: Array<String>): super() {
        this.permission = permission
    }

    constructor(permission: String, message: String): super(message) {
        this.permission = arrayOf(permission)
    }

    constructor(permission: Array<String>, message: String): super(message) {
        this.permission = permission
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermission(activity: Activity, requestCode: Int) {
        activity.requestPermissions(permission, requestCode)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestPermission(fragment: Fragment, requestCode: Int) {
        fragment.requestPermissions(permission, requestCode)
    }
}