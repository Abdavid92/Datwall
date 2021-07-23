package com.smartsolutions.paquetes.modules

import android.os.Build
import com.smartsolutions.paquetes.managers.*
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(ServiceComponent::class, ViewModelComponent::class, SingletonComponent::class)
class ManagersModule {

    @Provides
    fun provideNetworkUsageManager(
        impl: Lazy<NetworkUsageManagerDefault>,
        legacyImpl: Lazy<NetworkUsageManagerLegacy>
    ): NetworkUsageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            impl.get()
        else
            legacyImpl.get()
    }

    @Provides
    fun providePermissionsManager(
        impl: Lazy<PermissionsManagerM>,
        legacyImpl: Lazy<PermissionsManager>
    ): IPermissionsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            impl.get()
        else
            legacyImpl.get()
    }
}