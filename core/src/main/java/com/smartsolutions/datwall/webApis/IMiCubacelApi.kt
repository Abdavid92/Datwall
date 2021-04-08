package com.smartsolutions.datwall.webApis

import com.smartsolutions.datwall.webApis.models.MiCubacelAccount
import com.smartsolutions.datwall.webApis.models.Result

interface IMiCubacelApi {

    suspend fun signUp(account: MiCubacelAccount): Result<Any>

    suspend fun signIn(phone: String, password: String)
}