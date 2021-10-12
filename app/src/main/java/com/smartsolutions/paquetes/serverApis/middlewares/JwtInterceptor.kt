package com.smartsolutions.paquetes.serverApis.middlewares

import android.util.Log
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.serverApis.contracts.IJwtGenerator
import com.smartsolutions.paquetes.serverApis.models.JwtData
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class JwtInterceptor @Inject constructor(
    private val jwtGenerator: IJwtGenerator,
    private val jwtData: JwtData
) : Interceptor {

    private val tag = "JwtInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()

        val token = jwtGenerator.encode(jwtData)

        token?.let {
            try {
                builder.addHeader("Authorization", "Bearer $it")
            } catch (e: Exception) {
                Log.e(tag, "intercept: ${e.message}", e)
            }
        }
        builder.addHeader("Accept", "application/json")

        return chain.proceed(builder.build())
    }

}