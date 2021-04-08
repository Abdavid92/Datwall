package com.smartsolutions.datwall.webApis

import com.smartsolutions.datwall.webApis.models.MiCubacelAccount
import com.smartsolutions.datwall.webApis.models.Result
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import java.io.IOException
import javax.inject.Inject
import javax.net.ssl.SSLContext
import kotlin.coroutines.CoroutineContext

class MiCubacelApi @Inject constructor(
    private val sslContext: SSLContext
): IMiCubacelApi, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val timeout = 15000

    private val newAccountActions = arrayOf(
        "https://mi.cubacel.net:8443/login/NewUserRegistration",
        "https://mi.cubacel.net:8443/login/VerifyRegistrationCode"
    )

    override suspend fun signUp(account: MiCubacelAccount): Result<Any> {

        try {
            val connection = Jsoup.connect(newAccountActions[0])
                .sslSocketFactory(sslContext.socketFactory)
                .timeout(timeout)
                .data("msisdn", account.phone)
                .data("firstname", account.firstName)
                .data("lastname", account.lastName)
                .data("agree", "on")

            val document = connection.post()



        } catch (e: IOException) {

        }

        return Result.Success(null)
    }

    override suspend fun signIn(phone: String, password: String) {
        TODO("Not yet implemented")
    }
}