package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.managers.*
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider

@Module
@InstallIn(SingletonComponent::class)
class ManagersModule {

    @Provides
    fun provideNetworkUsageManager(
        impl: Provider<NetworkUsageManagerDefault>
    ): NetworkUsageManager {
        return impl.get()
    }
}