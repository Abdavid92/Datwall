package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.helpers.ChangeNetworkHelper
import com.smartsolutions.paquetes.helpers.IChangeNetworkHelper
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface ChangeNetworkHelperModule {

    @Binds
    fun bindIChangeNetworkHelper(impl: DatwallKernel): IChangeNetworkHelper
}