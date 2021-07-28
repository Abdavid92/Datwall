package com.smartsolutions.paquetes.modules

import android.content.Context
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.DatwallKernel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@Module
@InstallIn(ActivityComponent::class, ServiceComponent::class, ViewModelComponent::class)
class KernelModule {

    @Provides
    @Singleton
    fun provideKernel(@ApplicationContext context: Context): DatwallKernel {
        return (context as DatwallApplication).kernel
    }
}