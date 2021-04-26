package com.smartsolutions.paquetes.modules

import android.content.Context
import android.os.Build
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

    @Provides
    fun provideINetworkUsageManager(@ApplicationContext context: Context, trafficRepository: TrafficRepository): NetworkUsageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            NetworkUsageManagerDefault(context)
        else
            NetworkUsageManagerLegacy(trafficRepository)
    }
}