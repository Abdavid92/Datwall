package com.smartsolutions.datwall.modules

import android.content.Context
import android.os.Build
import com.smartsolutions.datwall.managers.*
import com.smartsolutions.datwall.repositories.TrafficRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class ManagersModule {

    @Provides
    fun provideIDataPackageManager(@ApplicationContext context: Context): IDataPackageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            DataPackageManager(context)
        else
            LegacyDataPackageManager(context)
    }

    @Provides
    fun provideINetworkUsageManager(@ApplicationContext context: Context, trafficRepository: TrafficRepository): NetworkUsageManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            NetworkUsageManagerDefault(context)
        else
            NetworkUsageManagerLegacy(trafficRepository)
    }
}