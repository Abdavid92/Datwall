package com.smartsolutions.paquetes.serverApis.contracts

import retrofit2.Call
import retrofit2.http.GET

interface ITimeApi {

    @GET("time")
    fun getServerTime(): Call<Long>
}