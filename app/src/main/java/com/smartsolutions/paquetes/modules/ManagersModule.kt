package com.smartsolutions.paquetes.modules

import android.content.Context
import android.os.Build
import com.smartsolutions.paquetes.helpers.SimsHelper
import com.smartsolutions.paquetes.managers.*
import com.smartsolutions.paquetes.repositories.TrafficRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ManagersModule {

    /*@Provides
    fun provideINetworkUsageManager(
        @ApplicationContext
        context: Context,
        trafficRepository: TrafficRepository,
        simsHelper: SimsHelper
    ): NetworkUsageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            NetworkUsageManagerDefault(context, simsHelper)
        else
            NetworkUsageManagerLegacy(trafficRepository)
    }*/

    @Provides
    fun provideINetworkUsageManager(
        impl: NetworkUsageManagerDefault,
        legacyImpl: NetworkUsageManagerLegacy
    ): NetworkUsageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            impl
        else
            legacyImpl
    }
}