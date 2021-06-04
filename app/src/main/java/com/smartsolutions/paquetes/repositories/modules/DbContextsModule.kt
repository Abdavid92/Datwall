package com.smartsolutions.paquetes.repositories.modules

import android.content.Context
import com.smartsolutions.paquetes.data.DbContext
import com.smartsolutions.paquetes.data.TrafficDbContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DbContextsModule {

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
    fun provideIPurchasedPackageDao(dbContext: DbContext) = dbContext.getPurchasedPackageDao()

    @Provides
    fun provideISimDao(dbContext: DbContext) = dbContext.getSimDao()

    @Provides
    fun provideISimDataPackageDao(dbContext: DbContext) = dbContext.getSimDataPackageDao()

    @Provides
    fun provideITrafficDao(dbContext: TrafficDbContext) = dbContext.getTrafficDao()
}