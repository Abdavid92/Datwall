package com.smartsolutions.paquetes.serverApis

import android.content.Context
import com.google.gson.Gson
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.serverApis.models.Result
import com.squareup.duktape.Duktape
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.Throws

class HttpClient @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    @Throws(Exception::class, HttpException::class, UnprocessableRequestException::class)
    suspend fun sendRequest(call: () -> Call<ResponseBody>): String {
        return suspendCoroutine {

            var completed = false

            for (i in 0..2) {
                if (completed) {
                    break
                }

                val callResponse = call()

                callResponse.enqueue(object : Callback<ResponseBody> {

                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            val body = response.body()?.string()

                            if (body != null && body.isNotEmpty()) {

                                if (body.contains("</html>")) {
                                    //Genero la cookies de nuevo
                                    generateCookies(body)
                                } else {
                                    it.resume(body)
                                    completed = true
                                }
                            } else {
                                it.resumeWithException(UnprocessableRequestException(UnprocessableRequestException.Reason.NO_RESPONSE))
                                completed = true
                            }
                        } else {
                            it.resumeWithException(HttpException(response))
                            completed = true
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, throwable: Throwable) {
                        if (i == 2) {
                            it.resumeWithException(throwable)
                        }
                    }
                })
            }

            if (!completed) {
                it.resumeWithException(UnprocessableRequestException(UnprocessableRequestException.Reason.NO_RESPONSE))
            }
        }
    }

    private fun generateCookies(body: String) {
        val a = getVar(body, "a")
        val b = getVar(body, "b")
        val c = getVar(body, "c")

        if (a != null && b != null && c != null) {
            val duktape = Duktape.create()
            val input = context.resources
                .openRawResource(R.raw.script)

            val script = input.bufferedReader()
                .readText()
            duktape.evaluate(script)

            val proxy = duktape.get("Proxy", Proxy::class.java)

            COOKIES = proxy.getCookieValue(a, b, c)
        }
    }

    private fun getVar(body: String, type: String): String? {
        val textIni = "=toNumbers(\""
        val textFinish = "\")"

        val ini = body.indexOf(type + textIni) + textIni.length + type.length
        val finish = body.indexOf(textFinish, ini)

        if (ini == -1 || finish == -1) {
            return null
        }
        return body.substring(ini, finish)
    }

    internal interface Proxy {
        fun getCookieValue(a: String, b: String, c: String): String
    }

    companion object {

        var COOKIES: String? = null

    }
}