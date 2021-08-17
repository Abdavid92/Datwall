package com.smartsolutions.paquetes.modules

import android.os.Build
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
        impl: Provider<NetworkUsageManagerDefault>,
        legacyImpl: Provider<NetworkUsageManagerLegacy>
    ): NetworkUsageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            impl.get()
        else
            legacyImpl.get()
    }

    @Provides
    fun providePermissionsManager(
        impl: Provider<PermissionsManagerM>,
        legacyImpl: Provider<PermissionsManager>
    ): IPermissionsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            impl.get()
        else
            legacyImpl.get()
    }
}