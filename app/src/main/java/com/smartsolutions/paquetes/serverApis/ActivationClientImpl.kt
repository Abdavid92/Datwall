package com.smartsolutions.paquetes.serverApis

import android.content.Context
import android.os.Build
import com.smartsolutions.paquetes.serverApis.contracts.IActivationClient
import com.smartsolutions.paquetes.serverApis.contracts.ISmartSolutionsApps
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import retrofit2.HttpException
import java.util.*
import javax.inject.Inject

class ActivationClientImpl @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val api: ISmartSolutionsApps
) : IActivationClient {

    override suspend fun getLicense(deviceId: String): Result<License> {
        try {
            val license = api.getLicense(context.packageName, deviceId)

            return Result.Success(license)
        } catch (e: Exception) {
            if (e is HttpException && e.code() == 404) {

                val license = License(
                    deviceId,
                    isPurchased = false,
                    isRestored = false,
                    Build.MANUFACTURER,
                    Build.MODEL,
                    null,
                    null,
                    Date(),
                    Date(),
                    context.packageName
                )

                val result = runCatching {
                    api.postLicense(license)
                }

                if (result.isSuccess)
                    return getLicense(deviceId)
                else {
                    result.exceptionOrNull()?.let {
                        return Result.Failure(it)
                    }
                }
            }

            return Result.Failure(e)
        }
    }

    override suspend fun updateLicense(license: License): Result<Unit> {

        if (license.packageName == null)
            license.packageName = context.packageName

        return try {
            api.putLicense(license.deviceId, license)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}