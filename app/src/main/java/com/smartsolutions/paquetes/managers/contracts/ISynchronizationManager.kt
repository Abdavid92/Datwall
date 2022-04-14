package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.repositories.models.Sim
import kotlin.jvm.Throws

/**
 * Manager de sincronización de los paquetes de datos del usuario.
 */
interface ISynchronizationManager {

    /**
     * Mode de sincronización (USSD, WebApi).
     */
    var synchronizationMode: IDataPackageManager.ConnectionMode

    /**
     * Indica si se deben usar las apis de ejecución de código  ussd de android 8.
     */
    var synchronizationUSSDModeModern: Boolean

    /**
     * Sincroniza los paquetes de datos del usuario.
     *
     * @param sim Linea que se va a sincronizar.
     */
    @Throws(Exception::class)
    suspend fun synchronizeUserDataBytes(sim: Sim)

    /**
     * Programa la sincronización automática.
     *
     * @param intervalInMinutes Intervali en minutos entre cada sincronización.
      */
    fun scheduleUserDataBytesSynchronization(intervalInMinutes: Int)

    /**
     * Cancela la sincronización automática.
     */
    fun cancelScheduleUserDataBytesSynchronization()
}