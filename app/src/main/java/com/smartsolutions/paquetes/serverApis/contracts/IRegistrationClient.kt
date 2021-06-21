package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result

/**
 * Cliente de registración del dispositivo.
 * */
interface IRegistrationClient {

    /**
     * Obtiene los datos del dispositivo en caso de que haya sido registrado
     * previamente.
     *
     * @param id - Identificador único del dispositivo.
     *
     * @return [Device] con los datos del dispositivo o null si no ha
     * sido registrado.
     * */
    suspend fun getRegisterDevice(id: String): Result<Device>

    suspend fun getOrRegister(device: Device): Result<Device>

    /**
     * Registra un nuevo dispositivo.
     *
     * @param device - Datos del dispositivo.
     *
     * @return `true` si tiene éxito.
     * */
    suspend fun registerDevice(device: Device): Result<Unit>

    /**
     * Actualiza los datos de un dispositivo previamente registrado.
     *
     * @param device - Datos del dispositivo.
     *
     * @return `true` si tiene éxito.
     * */
    suspend fun updateRegistration(device: Device): Result<Unit>

    suspend fun registerDeviceApp(deviceId: String, deviceApp: DeviceApp): Result<Unit>

    suspend fun getOrRegisterDeviceApp(id: String, deviceApp: DeviceApp): Result<DeviceApp>

    suspend fun updateDeviceApp(deviceApp: DeviceApp): Result<Unit>
}