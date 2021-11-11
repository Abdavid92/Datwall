package com.smartsolutions.paquetes.repositories.models

import androidx.annotation.Keep

@Keep
data class SpecialApp(
    val packageName: String,
    val name: String?,
    val allowAnnotations: String?,
    val blockedAnnotations: String?,
    val access: Boolean
)