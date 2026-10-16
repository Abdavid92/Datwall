package com.smartsolutions.paquetes.ui

import com.smartsolutions.paquetes.managers.contracts.IAppNavigator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Implementaciones de contratos del dominio que pertenecen a la UI.
 * */
@Module
@InstallIn(SingletonComponent::class)
abstract class UiBindingsModule {

    @Binds
    abstract fun bindAppNavigator(navigator: AppNavigator): IAppNavigator
}
