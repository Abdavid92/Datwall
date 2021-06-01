package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.Device
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

    /**
     * Registra un nuevo dispositivo.
     *
     * @param device - Datos del dispositivo.
     *
     * @return `true` si tiene éxito.
     * */
    suspend fun registerDevice(device: Device): Result<Unit>

    /**
     * Obtiene los datos del dispositivo o lo registra en caso de no
     * encontrarlo.
     *
     * @param device - Dispositivo.
     *
     * @return [Device] - Datos del dispositivo.
     * */
    suspend fun getOrRegister(device: Device): Result<Device>

    /**
     * Actualiza los datos de un dispositivo previamente registrado.
     *
     * @param device - Datos del dispositivo.
     *
     * @return `true` si tiene éxito.
     * */
    suspend fun updateRegistration(device: Device): Result<Unit>
}