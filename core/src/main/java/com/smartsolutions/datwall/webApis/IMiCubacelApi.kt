package com.smartsolutions.datwall.webApis

import com.smartsolutions.datwall.webApis.models.MiCubacelAccount
import com.smartsolutions.datwall.webApis.models.Result

interface IMiCubacelApi {

    suspend fun signUpNewUser(account: MiCubacelAccount): Result<Any>

    suspend fun signUpVerifyRegistration(code : String): Result<Any>

    suspend fun signUpEnterPassword(account: MiCubacelAccount) : Result<Any>

    suspend fun signIn(account: MiCubacelAccount) : Result<Any>
}