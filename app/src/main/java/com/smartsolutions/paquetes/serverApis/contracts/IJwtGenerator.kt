package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.JwtData

interface IJwtGenerator {

    fun encode(jwtData: JwtData): String?
}