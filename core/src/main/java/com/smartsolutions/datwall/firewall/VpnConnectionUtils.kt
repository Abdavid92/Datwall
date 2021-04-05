package com.smartsolutions.datwall.firewall

import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp

/**
 * Utilidades para el vpn
 * */
object VpnConnectionUtils {

    /**
     * Verifica si todas las aplicaciones de la lista tienen acceso
     *
     * @return true si todas tienen acceso, false en caso contrario
     * */
    fun allAccess(apps: List<IApp>): Boolean {
        apps.forEach {
            if (it is App && !it.access && !it.tempAccess) {
                return false
            } else if (it is AppGroup) {
                it.forEach { app ->
                    if (!app.access && !app.tempAccess)
                        return false
                }
            }
        }
        return true
    }
}