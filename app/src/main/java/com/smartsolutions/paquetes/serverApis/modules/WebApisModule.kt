package com.smartsolutions.paquetes.serverApis.modules

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.serverApis.contracts.ISmartSolutionsApps
import com.smartsolutions.paquetes.serverApis.middlewares.CookieJarProcessor
import com.smartsolutions.paquetes.serverApis.middlewares.JwtInterceptor
import com.smartsolutions.paquetes.serverApis.models.JwtData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(ActivityComponent::class)
class WebApisModule {

    @Provides
    fun provideJwtData(@ApplicationContext context: Context): JwtData {
        val split = String(Base64.decode(BuildConfig.CONTENT_0, Base64.DEFAULT)).split(";")

        return JwtData(
            split[0],
            context.packageName,
            BuildConfig.VERSION_CODE,
            split[1],
            split[2]
        )
    }

    @Provides
    fun provideISmartSolutionsApps(client: OkHttpClient): ISmartSolutionsApps =
        Retrofit.Builder()
            .baseUrl(BuildConfig.REGISTRATION_SERVER_URL)
            .client(client)
            .build()
            .create(ISmartSolutionsApps::class.java)

    @Provides
    fun provideOkHttpClient(interceptor: JwtInterceptor, sslContext: SSLContext): OkHttpClient {
        val trustManager = buildTrustManager()

        return OkHttpClient.Builder()
            .cookieJar(CookieJarProcessor())
            .addInterceptor(interceptor)
            .sslSocketFactory(sslContext.socketFactory, trustManager[0])
            .build()
    }

    @Provides
    fun provideSSLContext(trustAllCert: Array<X509TrustManager>): SSLContext {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCert, SecureRandom())
        return sslContext
    }

    @Provides
    fun provideTrustManager(): X509TrustManager = buildTrustManager()[0]

    private fun buildTrustManager(): Array<X509TrustManager> =
        arrayOf(object : X509TrustManager {
            @SuppressLint("TrustAllX509TrustManager")
            override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

            @SuppressLint("TrustAllX509TrustManager")
            override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return emptyArray()
            }
        })
}