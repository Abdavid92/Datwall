package com.smartsolutions.datwall.webApis.models

data class MiCubacelAccount(
    val firstName: String?,
    val lastName: String?,
    val phone: String,
    val password: String,
    val passwordConfirmation: String,
    val cookies: Map<String, String>?,
    val credit: Float?,
    val verified: Boolean = false
)
