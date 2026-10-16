package com.smartsolutions.paquetes.managers.models

import android.content.Context
import android.content.pm.PackageManager

/**
 * Permiso que necesita una función de la app. Solo describe el permiso: la UI pone sus
 * textos (por [requestCode]) y decide cómo pedirlo (por [kind]).
 * */
data class Permission(
    /**
     * Claves de los permisos del sistema; vacío en los permisos especiales.
     * */
    val keys: Array<String>,
    val requestCode: Int,
    val minSdk: Int,
    /**
     * Cómo se concede.
     * */
    val kind: PermissionKind = PermissionKind.RUNTIME,
    /**
     * Se pide en el primer uso. El resto se pide al usar la función que lo necesita.
     * */
    val atStartup: Boolean = false,
    val checkPermission: Permission.(context: Context) -> Boolean = { context ->
        keys.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Permission

        if (!keys.contentEquals(other.keys)) return false
        if (requestCode != other.requestCode) return false
        if (kind != other.kind) return false
        if (atStartup != other.atStartup) return false

        return true
    }

    override fun hashCode(): Int {
        var result = keys.contentHashCode()
        result = 31 * result + requestCode
        result = 31 * result + kind.hashCode()
        result = 31 * result + atStartup.hashCode()
        return result
    }
}
