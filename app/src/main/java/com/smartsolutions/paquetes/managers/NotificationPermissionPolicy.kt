package com.smartsolutions.paquetes.managers

import android.os.Build

/**
 * Indica si hay que pedir el permiso de notificaciones. Solo existe a partir
 * de Android 13 (API 33); antes las notificaciones están permitidas siempre.
 * */
fun isNotificationPermissionRequired(sdkInt: Int, granted: Boolean): Boolean =
    sdkInt >= Build.VERSION_CODES.TIRAMISU && !granted
