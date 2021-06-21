package com.smartsolutions.paquetes.serverApis

import com.google.gson.Gson
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.contracts.ISmartSolutionsApps
import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
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

    override suspend fun getOrRegister(device: Device): Result<Device> {
        return try {
            val result = httpDelegate.sendRequest { api.getOrRegister(device.id, device) }

            Result.Success(gson.fromJson(result, Device::class.java))
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun registerDevice(device: Device): Result<Unit> {
        return try {
            httpDelegate.sendRequest { api.registerDevice(device) }

            Result.Success(Unit)
        } catch (e: Exception) {
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

    override suspend fun registerDeviceApp(deviceId: String, deviceApp: DeviceApp): Result<Unit> {
        return try {
            httpDelegate.sendRequest { api.registerDeviceApp(deviceId, deviceApp) }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    override suspend fun getOrRegisterDeviceApp(id: String, deviceApp: DeviceApp): Result<DeviceApp> {
        return try {
            val result = httpDelegate.sendRequest { api.getOrRegisterDeviceApp(id, deviceApp) }

            Result.Success(gson.fromJson(result, DeviceApp::class.java))
        }catch (e: Exception){
            Result.Failure(e)
        }
    }

    override suspend fun updateDeviceApp(deviceApp: DeviceApp): Result<Unit> {
        return try {
            httpDelegate.sendRequest { api.updateDeviceApp(deviceApp.id, deviceApp) }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }
}