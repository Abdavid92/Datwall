package com.smartsolutions.paquetes.serverApis.models

data class JwtData(
        val name: String,
        val packageName: String,
        val version: Int,
        val audience: String,
        val key: String
)