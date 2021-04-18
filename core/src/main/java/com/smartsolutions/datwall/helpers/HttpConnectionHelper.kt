package com.smartsolutions.datwall.helpers

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.preferences.core.edit
import com.google.gson.Gson
import com.smartsolutions.datwall.PreferencesKeys
import com.smartsolutions.datwall.R
import com.smartsolutions.datwall.ResponseCallback
import com.smartsolutions.datwall.dataStore
import com.smartsolutions.datwall.exceptions.InternalServerError
import com.smartsolutions.datwall.exceptions.NotFoundException
import com.smartsolutions.datwall.exceptions.UnauthorizedException
import com.smartsolutions.datwall.exceptions.UnprocessableRequestException
import com.squareup.duktape.Duktape
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import okhttp3.ResponseBody
import retrofit2.Call
import java.io.IOException
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws

class HttpConnectionHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    val gson: Gson
): CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO


    inline fun sendRequestAsync(crossinline call: () -> Call<ResponseBody>, callback: ResponseCallback<String?>) {
        launch {
            try {
                callback.onSuccess(sendRequest(call))
            } catch (e: Exception) {
                callback.onFail(e)
            }
        }
    }

    @Throws(Exception::class)
    inline fun sendRequest(crossinline call: () -> Call<ResponseBody>): String? {
        //Hago tres intentos de llamadas
        for (i in 0..2) {

            //Creo la llamada
            val callResponse = call()

            //Ejecuto
            val response = callResponse.execute()

            //Si tuvo éxito
            if (response.isSuccessful) {
                //Obtengo el cuerpo
                val body = response.body()

                if (body != null) {
                    val s = body.string()

                    /*Si el cuerpo contiene html significa que se envió
                    * una respuesta incorrecta producto de que las cookies están obsoletas*/
                    if (s.contains("</html>")) {
                        //Genero la cookies de nuevo
                        generateCookies(s)
                    } else {
                        return s
                    }
                } else {
                    throw UnprocessableRequestException("Empty body")
                }
            } else if (i == 2) {
                throw when (response.code()) {
                    401 -> {
                        UnauthorizedException()
                    }
                    404 -> {
                        NotFoundException()
                    }
                    422 -> {
                        UnprocessableRequestException()
                    }
                    503,
                    500 -> {
                        InternalServerError()
                    }
                    else -> {
                        Exception()
                    }
                }
            }
        }

        return null
    }

    fun generateCookies(body: String) {
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

            runBlocking {
                context.dataStore.edit {
                    it[PreferencesKeys.DATWALL_COOKIES] = proxy.getCookieValue(a, b, c)
                }
            }
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
}