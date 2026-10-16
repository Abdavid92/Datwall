package com.smartsolutions.paquetes.repositories.modules

import android.content.Context
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.repositories.PreferencesRepository
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.uiDataStore
import com.smartsolutions.paquetes.workersDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreferencesModule {

    @Provides
    @Singleton
    fun providePreferencesRepository(@ApplicationContext context: Context): IPreferencesRepository =
        PreferencesRepository(
            context.settingsDataStore,
            context.internalDataStore,
            context.uiDataStore,
            context.workersDataStore,
            CoroutineScope(SupervisorJob() + Dispatchers.IO)
        )
}
