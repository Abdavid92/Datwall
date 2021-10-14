package com.smartsolutions.paquetes.serverApis.modules

import com.smartsolutions.paquetes.serverApis.ActivationClientImpl
import com.smartsolutions.paquetes.serverApis.DefaultJwtGenerator
import com.smartsolutions.paquetes.serverApis.RegistrationClientImpl
import com.smartsolutions.paquetes.serverApis.contracts.IActivationClient
import com.smartsolutions.paquetes.serverApis.contracts.IJwtGenerator
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface BindingsModule {

    @Binds
    fun bindIJwtGenerator(impl: DefaultJwtGenerator): IJwtGenerator

    @Binds
    fun bindIRegistrationClient(impl: RegistrationClientImpl): IRegistrationClient

    @Binds
    fun bindIActivationClient(impl: ActivationClientImpl): IActivationClient
}