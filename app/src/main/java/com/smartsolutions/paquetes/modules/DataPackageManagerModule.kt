package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.managers.DataPackageManager
import com.smartsolutions.paquetes.managers.IDataPackageManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface DataPackageManagerModule {

    @Binds
    fun bindIDataPackageManager(impl: DataPackageManager): IDataPackageManager
}