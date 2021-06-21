package com.smartsolutions.paquetes.serverApis

import android.content.Context
import android.util.Base64
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.serverApis.contracts.IJwtGenerator
import com.smartsolutions.paquetes.serverApis.contracts.ITimeApi
import com.smartsolutions.paquetes.serverApis.converters.LongConverterFactory
import com.smartsolutions.paquetes.serverApis.middlewares.CookieJarProcessor
import com.smartsolutions.paquetes.serverApis.models.JwtData
import dagger.hilt.android.qualifiers.ApplicationContext
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import okhttp3.OkHttpClient
import org.apache.commons.lang.time.DateUtils
import retrofit2.Retrofit
import java.util.*
import javax.inject.Inject
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class DefaultJwtGenerator @Inject constructor(
    @ApplicationContext
    private val context: Context,
    sslContext: SSLContext,
    trustManager: X509TrustManager
) : IJwtGenerator {

    private val api: ITimeApi

    init {
        val client = OkHttpClient.Builder()
                .cookieJar(CookieJarProcessor())
                .sslSocketFactory(sslContext.socketFactory, trustManager)
                .build()
        api = Retrofit.Builder()
                .baseUrl(BuildConfig.REGISTRATION_SERVER_URL)
                .addConverterFactory(LongConverterFactory())
                .client(client)
                .build()
                .create(ITimeApi::class.java)
    }

    override fun encode(jwtData: JwtData): String? {
        val iat = getServerTime()

        iat?.let {
            val exp = DateUtils.addMinutes(it, BuildConfig.TOKEN_DURATION)

            val claims = mutableMapOf<String, Any>().apply {
                put(context.getString(R.string.c_1), iat)
                put(context.getString(R.string.c_2), exp)
                put(context.getString(R.string.c_3), jwtData.audience)
                put(context.getString(R.string.c_4), jwtData.name)
                put(context.getString(R.string.c_5), jwtData.packageName)
                put(context.getString(R.string.c_6), jwtData.version)
                put(context.getString(R.string.c_7), jwtData.key)
            }

            val secretKey = Base64.decode(BuildConfig.SECRET_KEY, Base64.DEFAULT)

            val key = Keys.hmacShaKeyFor(secretKey)

            return Jwts.builder()
                    .setClaims(claims)
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact()
        }
        return null
    }

    private fun getServerTime(): Date? {
        return try {
            val response = api.getServerTime()
                    .execute()
            if (response.isSuccessful) {
                response.body()?.let {

                    return Date(it - 60000)
                }
            }
            return null
        } catch (e: Exception) {
            null
        }
    }
}