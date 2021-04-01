package com.smartsolutions.datwall.modules

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.smartsolutions.datwall.data.DbContext
import com.smartsolutions.datwall.data.IAppDao
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
    fun provideDbContext(@ApplicationContext context: Context): DbContext =
        DbContext.getInstance(context)

    @Provides
    fun provideIAppDao(dbContext: DbContext): IAppDao = dbContext.getAppDao()
}