package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.Device
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ISmartSolutionsApps {

    @POST("devices")
    fun registerDevice(@Body device: Device): Call<ResponseBody>

    @GET("devices/{id}")
    fun getDevice(@Path("id") id: String): Call<ResponseBody>

    @PUT("devices/{id}")
    fun updateDevice(@Path("id") id: String, @Body device: Device): Call<ResponseBody>
}