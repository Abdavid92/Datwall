package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.License
import retrofit2.http.*

interface ISmartSolutionsApps {

    @GET("licences/{package_name}/{device_id}")
    suspend fun getLicense(
        @Path("package_name")
        packageName: String,
        @Path("device_id")
        deviceId: String
    ): License

    @POST("licences")
    suspend fun postLicense(@Body license: License)

    @PUT("licences/{device_id}")
    suspend fun putLicense(@Path("device_id") deviceId: String, @Body license: License)
}