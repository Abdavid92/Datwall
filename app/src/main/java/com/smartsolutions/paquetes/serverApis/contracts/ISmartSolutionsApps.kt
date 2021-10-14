package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.License
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

    @POST("devices/{device}/deviceApps")
    fun registerDeviceApp(@Path("device") deviceId: String, @Body deviceApp: DeviceApp): Call<ResponseBody>

    @POST("devices/{device_id}")
    fun getOrRegister(@Path("device_id") deviceId: String, @Body device: Device): Call<ResponseBody>

    @POST("deviceApps/{id}")
    fun getOrRegisterDeviceApp(@Path("id") id: String, @Body deviceApp: DeviceApp): Call<ResponseBody>

    @PUT("deviceApps/{id}")
    fun updateDeviceApp(@Path("id") id: String, @Body deviceApp: DeviceApp): Call<ResponseBody>

    @GET("licences/{device_id}")
    suspend fun getLicense(@Path("device_id") deviceId: String): License

    @POST("licences")
    suspend fun postLicense(@Body license: License)

    @PUT("licences/{device_id}")
    suspend fun putLicense(@Path("device_id") deviceId: String, @Body license: License)
}