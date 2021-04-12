package com.smartsolutions.datwall.webApis.models

data class MiCubacelAccount(
    val firstName: String?,
    val lastName: String?,
    val phone: String,
    val password: String,
    val passwordConfirmation: String,
    var cookies: Map<String, String>?,
    val credit: Float?,
    var verified: Boolean = false
)
