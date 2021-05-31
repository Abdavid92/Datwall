package com.smartsolutions.paquetes.serverApis.contracts

import com.smartsolutions.paquetes.serverApis.models.Device

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
    suspend fun getRegisterDevice(id: String): Device?

    /**
     * Registra un nuevo dispositivo.
     *
     * @param device - Datos del dispositivo.
     * */
    suspend fun registerDevice(device: Device)

    /**
     * Actualiza los datos de un dispositivo previamente registrado.
     *
     * @param device - Datos del dispositivo.
     * */
    suspend fun updateRegistration(device: Device)
}