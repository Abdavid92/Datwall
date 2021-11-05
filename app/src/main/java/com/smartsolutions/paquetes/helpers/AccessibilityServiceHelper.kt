package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.services.UIScannerService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Se encarga de checkear y encender el servicio de accesibilidad.
 * */
class AccessibilityServiceHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    /**
     * Abre las configuraciones del dispositivo para activar los servicios de
     * accesibilidad.
     * */
    fun openAccessibilityServicesActivity() {

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    /**
     * Indica si el servicio está encendido y listo para trabajar.
     * */
    fun accessibilityServiceEnabled(): Boolean {
        return accessibilityServiceALive() && accessibilityServiceReady()
    }

    /**
     * Indica si el servicio está encendido pero no checkea si está listo.
     * */
    private fun accessibilityServiceALive(): Boolean {
        val pref = Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)

        val serviceName = UIScannerService::class.qualifiedName

        return pref != null &&
                pref.contains(context.packageName + "/" + serviceName)
    }

    /**
     * Indica si el servicio está listo.
     * */
    private fun accessibilityServiceReady(): Boolean {
        return (context.applicationContext as DatwallApplication)
            .uiScannerServiceReady
    }
}