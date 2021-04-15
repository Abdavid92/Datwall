package com.smartsolutions.datwall.managers

import android.util.Log
import com.smartsolutions.micubacel_client.MCubacelClient
import com.smartsolutions.micubacel_client.exceptions.UnprocessableRequestException
import kotlinx.coroutines.*
import org.jsoup.Connection
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
        sendRequests(1, {client.signUp(firstName, lastName, phone)}, callback)
    }


    fun verifyCode(code: String, callback: Callback<Any>) {
        sendRequests(9, {client.verifyCode(code)}, callback)
    }

    fun createPassword(password: String, cpassword: String, callback: Callback<Any>) {
        sendRequests(9, {client.createPassword(password, cpassword)}, callback)
    }



    private fun <T> sendRequests(attempt: Int, request: () -> T?, callback: Callback<T>) {
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

    interface Callback<T> {
        fun onSuccess(response: T)
        fun onFail(throwable: Throwable)
    }
}