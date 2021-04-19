package com.smartsolutions.datwall.helpers

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
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.jvm.Throws

/**
 * Clase de ayuda para realizar peticiones http a las apis alojadas
 * en servidores de infinityfreeapp.com
 * */
class HttpConnectionHelper @Inject constructor(
    @ApplicationContext
    private val context: Context
): CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO


    /**
     * Ejecuta una petición http de manera asíncrona. Retorna el resultado en un callback.
     *
     * @param call - Función que contruye la petición.
     * @param callback - Callback que resibe el resultado.
     * */
    inline fun sendRequestAsync(crossinline call: () -> Call<ResponseBody>, callback: ResponseCallback<String>) {
        launch {
            try {
                callback.onSuccess(sendRequest(call) ?: throw UnprocessableRequestException("Empty body"))
            } catch (e: Exception) {
                callback.onFail(e)
            }
        }
    }

    /**
     * Ejecuta una petición http y retorna el cuerpo de la respuesta o lanza
     * una excepción si no tuvo éxito.
     *
     * @param call - Función que contruye la petición.
     * @return Cuerpo de la respuesta en un String.
     * */
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
                /*Si la respuesta no tuvo éxito y es el último intento,
                * obtengo el código http y lanzo la excepción correspondiente*/
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

    /**
     * Ejecuta el script, genera las cookies y las
     * guardas en el dataStore.
     *
     * @param body - Código html con el script
     * */
    fun generateCookies(body: String) {
        //Obtengo el valor de las tres variables
        val a = getVar(body, "a")
        val b = getVar(body, "b")
        val c = getVar(body, "c")

        //Si logré obtenerlas
        if (a != null && b != null && c != null) {
            //Instancio el motor de javascript
            val duktape = Duktape.create()

            //Obtengo, leo y evaluo el script
            val input = context.resources
                .openRawResource(R.raw.script)

            val script = input.bufferedReader()
                .readText()
            duktape.evaluate(script)

            //Instancio la interfaz que servirá como proxy entre el código javascript y kotlin
            val proxy = duktape.get("Proxy", Proxy::class.java)

            runBlocking {
                context.dataStore.edit {
                    //Obtengo y guardo las cookies
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