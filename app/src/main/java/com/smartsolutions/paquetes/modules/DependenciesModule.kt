package com.smartsolutions.paquetes.modules

import com.smartsolutions.datwall.helpers.IChangeNetworkHelper
import com.smartsolutions.paquetes.helpers.ChangeNetworkHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DependenciesModule {

    @Binds
    fun bindIChangeNetworkHelper(impl: ChangeNetworkHelper): IChangeNetworkHelper
}