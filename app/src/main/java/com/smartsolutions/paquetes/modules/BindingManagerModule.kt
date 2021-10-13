package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.managers.*
import com.smartsolutions.paquetes.managers.contracts.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface BindingManagerModule {

    @Binds
    fun bindIDataPackageManager(impl: DataPackageManager): IDataPackageManager

    @Binds
    fun bindIIconManager(impl: IconManager): IIconManager

    @Binds
    fun bindIPurchasedPackagesManager(impl: PurchasedPackagesManager): IPurchasedPackagesManager

    @Binds
    fun bindISimManagerNew(impl: SimManager): ISimManager

    @Binds
    fun bindIStatisticsManager(impl: StatisticsManager): IStatisticsManager

    @Binds
    fun bindIUserDataBytesManager(impl: UserDataBytesManager): IUserDataBytesManager

    @Binds
    fun bindIActivationManager(impl: SampleActivationManager): IActivationManager

    @Binds
    fun bindIUpdateManager(impl: UpdateManager): IUpdateManager

    @Binds
    fun bindIConfigurationManager(impl: ConfigurationManager): IConfigurationManager

    @Binds
    fun bindISynchronizationManager(impl: SynchronizationManager): ISynchronizationManager

    @Binds
    fun bindIPermissionsManager(impl: PermissionsManager): IPermissionsManager
}