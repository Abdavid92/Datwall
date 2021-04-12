package com.smartsolutions.datwall.managers

import android.util.Log
import com.smartsolutions.micubacel_client.MCubacelClient
import kotlinx.coroutines.*
import org.jsoup.Connection
import javax.inject.Inject
import kotlin.Exception
import kotlin.coroutines.CoroutineContext

class MiCubacelClientManager @Inject constructor(private val client: MCubacelClient) : CoroutineScope {

    private val TAG = "MiCubacelClientManager"

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val jobs = mutableListOf<Job>()

    fun loadHomePage() {
        sendRequests(9, { client.resolveHomeUrl() }, object : Callback<String> {
            override fun onSuccess(response: String) {
                Log.i(TAG, "onSuccess: Coñoooo!!! $response")
                loadHomePage(response)
            }

            override fun onFail() {
                Log.i(TAG, "onFail: Noooo!!!!")
            }

        })
    }

    private fun loadHomePage(url: String) {
        sendRequests(9, { client.loadPage(url) }, object : Callback<Connection.Response> {
            override fun onSuccess(response: Connection.Response) {
                val result = client.readHomePage(response.parse())

                if (result.isEmpty())
                    Log.i(TAG, "onSuccess: result is empty")

                result.forEach {
                    Log.i(TAG, "onSuccess: key -> ${it.key}, value -> ${it.value}")
                }
            }

            override fun onFail() {

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
                                if (this.isActive)
                                    //withContext(Dispatchers.Main) {
                                        callback.onSuccess(response)
                                    //}
                            }
                            clearJobs()
                        }
                    } catch (e: Exception) {
                        if (fails + 1 == attempt) {
                            //withContext(Dispatchers.Main) {
                                callback.onFail()
                            //}
                            clearJobs()
                        } else
                            fails++
                    }
                }
            })
            Thread.sleep(500)
        }
    }

    private fun clearJobs() {
        jobs.forEach {
            if (!it.isCompleted)
                it.cancel()
        }
        jobs.clear()
    }

    interface Callback<T> {

        fun onSuccess(response: T)
        fun onFail()
    }
}