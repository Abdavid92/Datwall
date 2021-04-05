package com.smartsolutions.datwall.modules

import com.smartsolutions.datwall.repositories.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface RepositoriesModule {

    @Binds
    fun bindIAppRepository(impl: AppRepository): IAppRepository

    @Binds
    fun bindIDataPackageRepository(impl: DataPackageRepository): IDataPackageRepository

    @Binds
    fun bindIUserDataPackageRepository(impl:UserDataPackageRepository): IUserDataPackageRepository
}