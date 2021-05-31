package com.smartsolutions.paquetes.serverApis

import com.google.gson.Gson
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.contracts.ISmartSolutionsApps
import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.Result
import retrofit2.HttpException
import javax.inject.Inject

class RegistrationClientImpl @Inject constructor(
    private val api: ISmartSolutionsApps,
    private val httpClient: HttpClient,
    private val gson: Gson
) : IRegistrationClient {

    override suspend fun getRegisterDevice(id: String): Result<Device> {
        return try {
            val result = httpClient.sendRequest { api.getDevice(id) }

            Result.Success(gson.fromJson(result, Device::class.java))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun registerDevice(device: Device): Result<Unit> {
        return try {
            httpClient.sendRequest { api.registerDevice(device) }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getOrRegister(device: Device): Result<Device> {
        return try {
            val result = httpClient.sendRequest { api.getDevice(device.id) }

            Result.Success(gson.fromJson(result, Device::class.java))
        } catch (e: Exception) {

            if (e is HttpException && e.code() == 404) {

                if (registerDevice(device) is Result.Success) {
                    return Result.Success(device)
                }
            }
            Result.Failure(e)
        }
    }

    override suspend fun updateRegistration(device: Device): Result<Unit> {
        return try {
            httpClient.sendRequest { api.updateDevice(device.id, device) }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}