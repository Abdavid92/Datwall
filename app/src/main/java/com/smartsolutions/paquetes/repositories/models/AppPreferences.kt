package com.smartsolutions.paquetes.repositories.models

/**
 * Preferencias que usa la UI. Un valor null significa que el usuario no lo ha elegido
 * todavía: cada pantalla aplica su valor por defecto, como hasta ahora.
 * */
data class AppPreferences(
    val themeMode: Int? = null,
    val appTheme: Int? = null,
    val notificationClass: String? = null,
    val foregroundAppModern: Boolean? = null,
    val appsFilter: String? = null,
    val usagePeriod: Int? = null,
    val usageFilter: String? = null,
    val resumeFilter: String? = null,
    val bubbleSize: String? = null,
    val bubbleTransparency: Float? = null,
    val bubbleAlwaysShow: Boolean? = null,
    val dynamicFirewall: Boolean? = null,
    val dataDisclosureAnswered: Boolean? = null,
    val simSetupSkipped: Boolean? = null
)
