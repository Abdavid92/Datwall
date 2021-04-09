package com.smartsolutions.datwall.webApis

import com.smartsolutions.datwall.webApis.models.MiCubacelAccount
import com.smartsolutions.datwall.webApis.models.Result
import kotlinx.coroutines.*
import org.jsoup.Connection
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
        "https://mi.cubacel.net:8443/login/VerifyRegistrationCode",
        "https://mi.cubacel.net:8443/login/recovery/RegisterPasswordCreation"
    )

    private var cookies : MutableMap<String, String>  = mutableMapOf()

    override suspend fun signUpNewUser(account: MiCubacelAccount): Result<Any> {

        try {
            val connection = jsoupConnect(newAccountActions[0])
                .data("msisdn", account.phone)
                .data("firstname", account.firstName)
                .data("lastname", account.lastName)
                .data("agree", "on")

            val document = connection.post()

            val elements = document.getElementsByAttributeValue("action", "/login/VerifyRegistrationCode")

            if (elements != null && elements.size > 0){
                cookies = connection.response().cookies()
            }else{
                return Result.Fail("No se pudo conectar")
            }

        } catch (e: IOException) {
            return Result.Fail(e.message!!)
        }

        return Result.Success(null)
    }

    override suspend fun signUpVerifyRegistration(code : String): Result<Any> {
        try {
            val connection = jsoupConnect(newAccountActions[1])
                .cookies(cookies)
                .data("username", code)

            val document = connection.post()

            val elements = document.getElementsByAttributeValue("action", "https://mi.cubacel.net:8443/login/recovery/RegisterPasswordCreation")

            if (elements != null && elements.size > 0){
                cookies = connection.response().cookies()
                return Result.Success(null)
            }else {
                return Result.Fail("Codigo Incorrecto")
            }

        }catch (e : IOException){
            return Result.Fail(e.message!!)
        }
    }

    override suspend fun signUpEnterPassword(account: MiCubacelAccount) : Result<Any> {
        try {
            val connection = jsoupConnect(newAccountActions[2])
                .cookies(cookies)
                .data("newPassword", account.password)
                .data("cnewPassword", account.passwordConfirmation)

            val document = connection.post()

            val elements = document.getElementsByAttributeValue(
                "action",
                "https://mi.cubacel.net:8443/login/jsp/register-confirmation.jsp?language=es#"
            )

            if (elements != null && elements.size > 0){
                cookies = connection.response().cookies()
                return Result.Success(null)
            }else {
                return Result.Fail("No se pudo crear la cuenta")
            }

        }catch (e : IOException){
            return Result.Fail(e.message!!)
        }
    }


    override suspend fun signIn(account: MiCubacelAccount) : Result<Any> {
        TODO("Not yet implemented")
    }


    private fun jsoupConnect(url : String) : Connection{
       return Jsoup.connect(url)
            .header("Accept-Language", "es")
            .sslSocketFactory(sslContext.socketFactory)
            .timeout(timeout)
    }
}