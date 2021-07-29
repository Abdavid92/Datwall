package com.smartsolutions.paquetes.modules

import android.content.Context
import com.google.gson.Gson
import com.smartsolutions.paquetes.helpers.NotificationHelper
import dagger.Binds
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

}