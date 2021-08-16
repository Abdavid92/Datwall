package com.smartsolutions.paquetes.modules

import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DependenciesModule {

    @Provides
    fun provideGson() = Gson()
}