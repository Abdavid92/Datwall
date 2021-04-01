package com.smartsolutions.datwall.modules

import com.smartsolutions.datwall.repositories.AppRepository
import com.smartsolutions.datwall.repositories.IAppRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoriesModule {

    @Binds
    fun bindIAppRepository(impl: AppRepository): IAppRepository
}