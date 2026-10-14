package com.smartsolutions.paquetes.managers.models

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.fragment.app.Fragment

data class Permission(
    val name: String,
    val keys: Array<String>,
    val description: String,
    val requestCode: Int,
    val minSdk: Int,
    /**
     * Se pide en el primer uso. El resto se pide al usar la función que lo necesita.
     * */
    val atStartup: Boolean = false,
    val checkPermission: Permission.(context: Context) -> Boolean = { context ->

        var result = true

        for (key in keys) {
            if (context.checkSelfPermission(key) == PackageManager.PERMISSION_DENIED) {
                result = false
                break
            }
        }
        result
    },
    val requestPermissionActivity: Permission.(activity: Activity) -> Unit = { activity ->
        activity.requestPermissions(keys, requestCode)
    },
    val requestPermissionFragment: Permission.(fragment: Fragment) -> Unit = { fragment ->
        fragment.requestPermissions(keys, requestCode)
    }
) {

    var intent: Intent? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Permission

        if (name != other.name) return false
        if (!keys.contentEquals(other.keys)) return false
        if (description != other.description) return false
        if (atStartup != other.atStartup) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + keys.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + atStartup.hashCode()
        return result
    }
}