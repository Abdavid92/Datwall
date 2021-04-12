package com.smartsolutions.datwall.webApis

import android.content.Context
import com.smartsolutions.datwall.R
import com.smartsolutions.datwall.webApis.models.MiCubacelAccount
import com.smartsolutions.datwall.webApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import javax.inject.Inject
import javax.net.ssl.SSLContext
import kotlin.coroutines.CoroutineContext

class MiCubacelApi @Inject constructor(
    @ApplicationContext
    private val context: Context,
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

    private val loginAction = "https://mi.cubacel.net:8443/login/Login"

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
                return Result.Fail(context.getString(R.string.sign_up_fail))
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

            return if (elements != null && elements.size > 0){
                cookies = connection.response().cookies()
                Result.Success(null)
            }else {
                Result.Fail(context.getString(R.string.sign_up_wrong_code))
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

            return if (elements != null && elements.size > 0){
                cookies = connection.response().cookies()
                Result.Success(null)
            }else {
                Result.Fail(context.getString(R.string.sign_up_fail))
            }

        }catch (e : IOException){
            return Result.Fail(e.message!!)
        }
    }


    override suspend fun signIn(account: MiCubacelAccount) : Result<Document> {
        val connection = jsoupConnect(loginAction)
            .data("username", account.phone)
            .data("password", account.password)

        val document = connection.post()

        return if (!isErrorPage(document)) {

            cookies = connection.response().cookies()
            account.cookies = cookies
            account.verified = true

            Result.Success(document)
        } else
            Result.Fail(context.getString(R.string.fail_sign_in))
    }


    private fun jsoupConnect(url : String) : Connection {
       return Jsoup.connect(url)
            .header("Accept-Language", "es")
            .sslSocketFactory(sslContext.socketFactory)
            .timeout(timeout)
    }

    private fun isErrorPage(document: Document): Boolean {
        val errorPage = document.getElementsByClass("error_page")

        val errorBlocking = document.getElementsByClass("error_Block")

        return errorPage.isNotEmpty() && errorBlocking.isNotEmpty()
    }
}