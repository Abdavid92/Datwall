package com.smartsolutions.datwall.managers

import android.util.Log
import com.smartsolutions.micubacel_client.MCubacelClient
import kotlinx.coroutines.*
import org.jsoup.Connection
import javax.inject.Inject
import kotlin.Exception
import kotlin.coroutines.CoroutineContext

class MiCubacelClientManager() : CoroutineScope {

    private val client = MCubacelClient()

    private val TAG = "EJV"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val jobs = mutableListOf<Job>()

    fun loadHomePage() {
        sendRequests(9, { client.resolveHomeUrl() }, object : Callback<String> {
            override fun onSuccess(response: String) {
                Log.i(TAG, "onSuccess: Yesss!!! $response jobs ${jobs.size}")
                loadHomePage(response)
            }

            override fun onFail() {
                Log.i(TAG, "onFail: Noooo!!!!  jobs ${jobs.size}")
            }

        })
    }

    private fun loadHomePage(url: String) {
        sendRequests(9, { client.loadPage(url) }, object : Callback<Connection.Response> {
            override fun onSuccess(response: Connection.Response) {
                val result = client.readHomePage(response.parse())

                if (result.isEmpty())
                    Log.i(TAG, "onSuccess: result is empty  jobs ${jobs.size}")

                result.forEach {
                    Log.i(TAG, "onSuccess: key -> ${it.key}, value -> ${it.value}  jobs ${jobs.size}")
                }
            }

            override fun onFail() {
                Log.i(TAG, "FAIL  jobs ${jobs.size}")
            }
        })
    }

    private fun <T> sendRequests(attempt: Int, request: () -> T?, callback: Callback<T>) {
        var fails = 1
        var onSuccess = false

        for (i in 1..attempt) {

            if (onSuccess)
                break

            jobs.add(launch {
                if (this.isActive) {
                    try {
                        if (!onSuccess) {
                            val response = request() ?: throw NullPointerException()
                            if (!onSuccess) {
                                onSuccess = true
                                clearJobs()
                                Log.i(TAG, "success $i  jobs ${jobs.size}")
                                callback.onSuccess(response)
                            }
                        }
                    } catch (e: Exception) {
                        Log.i(TAG, "fail $i error: ${e.message}  jobs ${jobs.size}")
                        if (fails + 1 == attempt && !onSuccess) {
                            clearJobs()
                            callback.onFail()
                        } else
                            fails++
                    }
                }
            })
            if (onSuccess)
                break
            Thread.sleep(1000)
        }
    }

    private fun clearJobs() {
        jobs.forEach {
            it.cancel()
        }
        jobs.clear()
    }

    interface Callback<T> {
        fun onSuccess(response: T)
        fun onFail()
    }
}