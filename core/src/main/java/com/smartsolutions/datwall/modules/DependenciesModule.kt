package com.smartsolutions.datwall.modules

import android.annotation.SuppressLint
import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.smartsolutions.datwall.data.DbContext
import com.smartsolutions.datwall.data.IAppDao
import com.smartsolutions.datwall.data.TrafficDbContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.X509TrustManager

@Module
@InstallIn(SingletonComponent::class)
object DependenciesModule {

    @Provides
    fun provideGson() = Gson()

    @Provides
    fun provideDbContext(@ApplicationContext context: Context) =
        DbContext.getInstance(context)

    @Provides
    fun provideTrafficDbContext(@ApplicationContext context: Context) =
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
    fun provideSslSocketFactory(): SSLContext {
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, buildTrustManager(), SecureRandom())
        return sslContext
    }

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