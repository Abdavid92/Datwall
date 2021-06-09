package com.smartsolutions.paquetes.modules

import com.smartsolutions.paquetes.managers.DataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.UserDataBytesManagerOld
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface IManagerModule {

    @Binds
    fun bindIDataPackageManager(impl: DataPackageManager): IDataPackageManager

    @Binds
    fun bindIUserDataBytesManager(impl: UserDataBytesManagerOld): IUserDataBytesManager
}