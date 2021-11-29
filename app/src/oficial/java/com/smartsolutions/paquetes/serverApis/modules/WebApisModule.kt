package com.smartsolutions.paquetes.serverApis.modules

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.serverApis.contracts.ISmartSolutionsApps
import com.smartsolutions.paquetes.serverApis.middlewares.JwtInterceptor
import com.smartsolutions.paquetes.serverApis.models.JwtData
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@Module
@InstallIn(SingletonComponent::class)
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
    fun provideISmartSolutionsApps(client: OkHttpClient, gson: Gson): ISmartSolutionsApps =
        Retrofit.Builder()
            .baseUrl(BuildConfig.REGISTRATION_SERVER_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ISmartSolutionsApps::class.java)

    @Provides
    fun provideOkHttpClient(interceptor: JwtInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()
    }
}