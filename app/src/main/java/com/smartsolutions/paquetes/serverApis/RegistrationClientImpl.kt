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
    private val httpDelegate: HttpDelegate,
    private val gson: Gson
) : IRegistrationClient {

    override suspend fun getRegisterDevice(id: String): Result<Device> {
        return try {
            val result = httpDelegate.sendRequest { api.getDevice(id) }

            Result.Success(gson.fromJson(result, Device::class.java))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun registerDevice(device: Device): Result<Unit> {
        return try {
            httpDelegate.sendRequest { api.registerDevice(device) }

            device.deviceApps?.forEach {
                httpDelegate.sendRequest { api.registerDeviceApp(device.id, it) }
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getOrRegister(device: Device): Result<Device> {
        return try {
            val result = getRegisterDevice(device.id)

            if (result.isSuccess)
                return result
            else
                throw (result as Result.Failure).throwable
        } catch (e: Exception) {

            if (e is HttpException && e.code() == 404) {

                if (registerDevice(device) is Result.Success) {
                    return getRegisterDevice(device.id)
                }
            }
            Result.Failure(e)
        }
    }

    override suspend fun updateRegistration(device: Device): Result<Unit> {
        return try {
            httpDelegate.sendRequest { api.updateDevice(device.id, device) }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}