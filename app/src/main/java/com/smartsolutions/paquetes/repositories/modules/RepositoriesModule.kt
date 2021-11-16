package com.smartsolutions.paquetes.repositories.modules

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
    fun bindISimRepository(impl: SimRepository): ISimRepository

    @Binds
    fun bindITrafficRepository(impl: TrafficRepository): ITrafficRepository

    @Binds
    fun bindIUsageGeneralRepository(impl: UsageGeneralRepository): IUsageGeneralRepository

    /*@Binds
    fun bindIEventRepository(impl: EventRepository): IEventRepository*/
}