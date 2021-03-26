package com.smartsolutions.paquetes.repositories.models

data class SpecialApp(
    val packageName: String,
    val allowAnnotations: String?,
    val blockedAnnotations: String?,
    val access: Boolean
)