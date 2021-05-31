package com.smartsolutions.paquetes.serverApis.modules

import com.smartsolutions.paquetes.serverApis.DefaultJwtGenerator
import com.smartsolutions.paquetes.serverApis.contracts.IJwtGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
interface BindingsModule {

    @Binds
    fun bindIJwtGenerator(impl: DefaultJwtGenerator): IJwtGenerator
}