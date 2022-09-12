package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.managers.models.Update

/**
 * Busca y descarga las actualizaciones.
 * */
interface IUpdateManager {

    /**
     * Busca una actualización en el servidor de Apklis.
     *
     * @return [Update] si encontró una actualización.
     * */
    suspend fun findUpdate(): Update?

    /**
     * Programa la búsqueda de actualizaciones periodica.
     *
     * @param intervalInHours - Intervalo en horas.
     * */
    fun scheduleUpdateApplicationStatusWorker(intervalInHours: Long)

    /**
     * Cancela los workers si estaban registrados
     */
    fun cancelUpdateApplicationStatusWorker()

    /**
     * Indica si ya fué registrado el worker de actualización de estado.
     * */
    fun wasScheduleUpdateApplicationStatusWorker(): Boolean


}