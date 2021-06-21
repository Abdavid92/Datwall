package com.smartsolutions.paquetes.annotations

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD, AnnotationTarget.VALUE_PARAMETER)
@StringDef(ApplicationStatus.ACTIVATED, ApplicationStatus.DEACTIVATED, ApplicationStatus.DISCONTINUED)
annotation class ApplicationStatus {
    companion object {
        const val ACTIVATED = "activated"
        const val DEACTIVATED = "deactivated"
        const val DISCONTINUED = "discontinued"
    }
}
