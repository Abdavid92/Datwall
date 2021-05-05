package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.repositories.*
import com.smartsolutions.paquetes.repositories.contracts.*
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
    fun bindIPurchasedPackageRepository(impl: PurchasedPackageRepository): IPurchasedPackageRepository

    @Binds
    fun bindIUserDataBytesRepository(impl: UserDataBytesRepository): IUserDataBytesRepository

    @Binds
    fun bindIMiCubacelAccountRepository(impl: MiCubacelAccountRepository): IMiCubacelAccountRepository
}