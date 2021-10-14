package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result

interface IActivationClient {

    suspend fun getLicense(deviceId: String): Result<License>

    suspend fun updateLicense(license: License): Result<Unit>
}