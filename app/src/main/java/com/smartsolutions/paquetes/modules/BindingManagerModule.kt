package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.managers.*
import com.smartsolutions.paquetes.managers.contracts.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(ServiceComponent::class, ViewModelComponent::class, SingletonComponent::class)
interface BindingManagerModule {

    @Binds
    fun bindIDataPackageManager(impl: DataPackageManager): IDataPackageManager

    @Binds
    fun bindIIconManager(impl: IconManager): IIconManager

    @Binds
    fun bindIMiCubacelManager(impl: MiCubacelManager): IMiCubacelManager

    @Binds
    fun bindIPurchasedPackagesManager(impl: PurchasedPackagesManager): IPurchasedPackagesManager

    @Binds
    fun bindISimManager(impl: SimManager): ISimManager

    @Binds
    fun bindIStatisticsManager(impl: StatisticsManager): IStatisticsManager

    @Binds
    fun bindIUserDataBytesManager(impl: UserDataBytesManager): IUserDataBytesManager

    @Binds
    fun bindIActivationManager(impl: ActivationManager): IActivationManager
}