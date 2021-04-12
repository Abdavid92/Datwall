package com.smartsolutions.datwall.managers

import com.smartsolutions.micubacel_client.MCubacelClient
import kotlinx.coroutines.*
import java.lang.Exception
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.net.ssl.SSLHandshakeException
import kotlin.coroutines.CoroutineContext

class MiCubacelClientManager @Inject constructor(private val client: MCubacelClient) : CoroutineScope {

    private val jobs = mutableListOf<Job>()

    fun loadHomePage() {
        for (it in 1 .. 9){
            jobs.add(launch {
                try {
                    val url = client.resolveHomeUrl()
                    if (url != null){

                    }
                } catch (e: Exception) {

                }
            })
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

}