package com.smartsolutions.datwall.webApis

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST


interface DatwallWebApi {

    @GET("dataPackage")
    fun getPackages(): Call<ResponseBody>
}