package com.smartsolutions.paquetes.managers

import android.util.Log
import com.smartsolutions.paquetes.repositories.models.UserDataPackage
import com.smartsolutions.micubacel_client.MCubacelClient
import com.smartsolutions.micubacel_client.exceptions.UnprocessableRequestException
import kotlinx.coroutines.*
import org.jsoup.nodes.Document
import kotlin.Exception
import kotlin.coroutines.CoroutineContext

class MiCubacelClientManager() : CoroutineScope {

    private val client = MCubacelClient()

    private val TAG = "EJV"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    fun loadHomePage(callback: Callback<Map<String, String>>, updateCookies : Boolean = true) {
        sendRequests(9, { client.resolveHomeUrl(updateCookies) }, object : Callback<String> {
            override fun onSuccess(response: String) {
                loadHomePage(response, callback)
            }

            override fun onFail(throwable: Throwable) {
                callback.onFail(throwable)
            }

        })
    }

    private fun loadHomePage(url: String, callback: Callback<Map<String, String>>) {
        sendRequests(9, { client.loadPage(url) }, object : Callback<Document> {
            override fun onSuccess(response: Document) {
                val result = client.readHomePage(response)
                if (result.isEmpty()){
                    callback.onFail(UnprocessableRequestException())
                }else {
                    callback.onSuccess(result)
                }
            }

            override fun onFail(throwable: Throwable) {
                callback.onFail(throwable)
            }
        })
    }

    fun signIn(phone: String, password: String, callback : Callback<Any>) {
        sendRequests(9, {client.signIn(phone, password)}, callback)
    }



    fun signUp(firstName : String, lastName : String, phone: String, callback: Callback<Any>) {
        sendQueueRequests(9, {client.signUp(firstName, lastName, phone)}, callback)
    }


    fun verifyCode(code: String, callback: Callback<Any>) {
        sendRequests(9, {client.verifyCode(code)}, callback)
    }

    fun createPassword(password: String, callback: Callback<Any>) {
        sendRequests(9, {client.createPassword(password)}, callback)
    }


    fun getUserDataPackagesInfo(userDataPackage: UserDataPackage?, callback: Callback<UserDataPackage>) {
        Log.i("EJV", "Enviando peticion de paquetes")
        sendRequests(9, { client.obtainPackagesInfo() }, object : Callback<Map<String, Double>> {
            override fun onSuccess(response: Map<String, Double>) {
                Log.i(TAG, "onSuccess: éxito")

                val dataPackage = userDataPackage?.copy(
                    bytes = response[MCubacelClient.DATA_BYTES]!!.toLong()
                )
                    ?: UserDataPackage(
                        0,
                        response[MCubacelClient.DATA_BYTES]?.toLong() ?: 0,
                        response[MCubacelClient.DATA_BONUS_BYTES]?.toLong() ?: 0,
                        response[MCubacelClient.DATA_BONUS_CU_BYTES]?.toLong() ?: 0,
                        0, 0, true, 1, 0
                    )

                callback.onSuccess(dataPackage)
            }

            override fun onFail(throwable: Throwable) {
                if (throwable is UnprocessableRequestException) {
                    Log.i("EJV", "No se ha iniciado sesion")
                } else {
                    Log.i("EJV", "Fallido")
                }

                callback.onFail(throwable)
            }
        })
    }



    /**
     * Ejecuta una cantidad específica de peticiones http de manera paralela.
     * Cuando se recibe la primera respuesta, se cancelan todas las demas peticiones y
     * se lanzan los eventos correspondiente del callback.
     * En caso de no recibir respuesta a ninguna de las peticiones realizadas,
     * se lanza el evento fail del callback.
     *
     * @param attempt - Número de peticiones a ejecutar.
     * @param request - Función que contiene el código de la petición http.
     * @param callback - Callback que recibirá la respuesta o el error.
     * @param T - Tipo de respuesta que retorna la petición.
     * */
    private inline fun <T> sendRequests(attempt: Int, crossinline request: () -> T?, callback: Callback<T>) {
        launch {
            var fails = 1
            var completed = false

            for (i in 1..attempt) {
                launch {
                    if (this.isActive) {
                        try {
                            if (!completed) {
                                val response = request() ?: throw NullPointerException()
                                if (!completed) {
                                    completed = true
                                    Log.i(TAG, "success $i ")
                                    withContext(Dispatchers.Main){
                                        callback.onSuccess(response)
                                    }
                                }
                            }
                        } catch (e: UnprocessableRequestException){
                            if (!completed) {
                                completed = true
                                withContext(Dispatchers.Main){
                                    callback.onFail(e)
                                }
                            }
                        }catch (e: Exception) {
                            if (fails + 1 == attempt && !completed) {
                                completed = true
                                Log.i(TAG, "fail $fails error: ${e.message} ")
                                withContext(Dispatchers.Main){
                                    callback.onFail(e)
                                }
                            } else
                                fails++
                        }
                    }
                }
                if (completed)
                    break
                delay(1000L)
            }
        }
    }

    /**
     * Ejecuta una petición http una cantidad de veces determinada mientras no tenga éxito.
     * Deja de ejecutar la peticion si se supera la cantidad de intentos o  si la peticion
     * tiene respuesta.
     *
     * @param attempt - Número de intentos.
     * @param request - Petición http.
     * @param callback - Callback que recibirá la respuesta o el error.
     * */
    private inline fun <T> sendQueueRequests(attempt: Int, crossinline request: () -> T?, callback: Callback<T>) {
        launch {

            for (i in 1..attempt) {

                try {

                    val response = request() ?: throw NullPointerException()

                    withContext(Dispatchers.Main) {
                        callback.onSuccess(response)
                    }
                    break
                } catch (e: UnprocessableRequestException) {
                    withContext(Dispatchers.Main) {
                        callback.onFail(e)
                    }
                    break
                } catch (e: Exception) {
                    if (i == attempt) {
                        withContext(Dispatchers.Main) {
                            callback.onFail(e)
                        }
                    }
                }

                delay(1000)
            }
        }
    }

    interface Callback<T> {
        fun onSuccess(response: T)
        fun onFail(throwable: Throwable)
    }
}