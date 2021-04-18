package com.smartsolutions.datwall.modules

import android.annotation.SuppressLint
import android.content.Context
import com.google.gson.Gson
import com.smartsolutions.datwall.data.*
import com.smartsolutions.datwall.dataStore
import com.smartsolutions.datwall.interceptors.CookieJarProcessor
import com.smartsolutions.datwall.webApis.DatwallWebApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object DependenciesModule {

    @Provides
    fun provideGson() = Gson()

    @Provides
    fun provideDbContext(@ApplicationContext context: Context): DbContext =
        DbContext.getInstance(context)

    @Provides
    fun provideTrafficDbContext(@ApplicationContext context: Context): TrafficDbContext =
        TrafficDbContext.getInstance(context)

    @Provides
    fun provideIAppDao(dbContext: DbContext) = dbContext.getAppDao()

    @Provides
    fun provideIDataPackageDao(dbContext: DbContext) = dbContext.getDataPackageDao()

    @Provides
    fun provideIUserDataPackageDao(dbContext: DbContext) = dbContext.getUserDataPackageDao()

    @Provides
    fun provideITrafficDao(dbContext: TrafficDbContext) = dbContext.getTrafficDao()

    @Provides
    fun provideDatwallWebApi(client: OkHttpClient): DatwallWebApi {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8000/api/")
            .client(client)
            .build()
            .create(DatwallWebApi::class.java)
    }

    @Provides
    fun provideOkHttpClient(
        @ApplicationContext
        context: Context,
        sslContext: SSLContext,
        trustManager: Array<X509TrustManager>
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustManager[0])
            .cookieJar(CookieJarProcessor(context.dataStore))
            .build()
    }

    @Provides
    fun provideSslSocketFactory(trustManager: Array<X509TrustManager>): SSLContext {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustManager, SecureRandom())
        return sslContext
    }

    @Provides
    fun provideTrustManager(): Array<X509TrustManager> =
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