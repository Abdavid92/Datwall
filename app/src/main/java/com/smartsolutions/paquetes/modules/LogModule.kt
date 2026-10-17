package com.smartsolutions.paquetes.modules

import com.abdavid92.persistentlog.LogManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object LogModule {

    /**
     * Registro de eventos de la app. Se inyecta para poder sustituirlo en las pruebas de las pantallas
     * que lo muestran.
     * */
    @Provides
    fun provideLogManager(): LogManager = LogManager.newInstance()
}
