package com.smartsolutions.paquetes.managers.models

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.fragment.app.Fragment

data class Permission(
    val name: String,
    val keys: Array<String>,
    val description: String,
    val category: Category,
    val requestCode: Int,
    val minSdk: Int,
    val checkPermission: Permission.(context: Context) -> Boolean = { context ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            var result = true

            for (key in keys) {
                if (context.checkSelfPermission(key) == PackageManager.PERMISSION_DENIED) {
                    result = false
                    break
                }
            }
            result
        } else
            true
    },
    val requestPermissionActivity: Permission.(activity: Activity) -> Unit = { activity ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            activity.requestPermissions(keys, requestCode)
    },
    val requestPermissionFragment: Permission.(fragment: Fragment) -> Unit = { fragment ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            fragment.requestPermissions(keys, requestCode)
    }
) {

    var intent: Intent? = null

    enum class Category {
        Required,
        Optional
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Permission

        if (name != other.name) return false
        if (!keys.contentEquals(other.keys)) return false
        if (description != other.description) return false
        if (category != other.category) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + keys.contentHashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + category.hashCode()
        return result
    }
}