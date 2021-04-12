package com.smartsolutions.datwall.modules

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ApisModule {

    @Binds
    fun bindIMiCubacelApi(miCubacelApi: MiCubacelApi) : IMiCubacelApi

}