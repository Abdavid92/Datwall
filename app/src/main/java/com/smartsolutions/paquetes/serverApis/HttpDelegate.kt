package com.smartsolutions.paquetes.serverApis

import android.content.Context
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

class HttpDelegate @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    /**
     * Hace ejecuta una petición http y verifica si tuvo éxito.
     *
     * @param call - Petición http.
     *
     * @return El cuerpo de la respuesta o null si la petición
     * se realizó correctamente pero no hubo cuerpo.
     *
     * @throws Exception - si la petición falla.
     * */
    @Throws(Exception::class)
    fun sendRequest(call: () -> Call<ResponseBody>): String? {

        for (i in 0..2) {
            val response = call().execute()

            if (response.isSuccessful) {
                val body = response.body()?.string()

                if (body != null) {
                    if (body.contains("</html>")) {
                        generateCookies(body)
                        continue
                    }
                    return body
                }
            } else {
                throw HttpException(response)
            }
            break
        }
        return null
    }

    private fun generateCookies(body: String) {
        /*val a = getVar(body, "a")
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
        }*/
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