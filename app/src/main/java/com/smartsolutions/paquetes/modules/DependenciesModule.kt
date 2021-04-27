package com.smartsolutions.paquetes.modules

import android.content.Context
import com.google.gson.Gson
import com.smartsolutions.paquetes.data.DbContext
import com.smartsolutions.paquetes.data.TrafficDbContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

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
    fun provideITrafficDao(dbContext: TrafficDbContext) = dbContext.getTrafficDao()

    @Provides
    fun provideIPurchasedPackageDao(dbContext: DbContext) = dbContext.getPurchasedPackageDao()
}