package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.Throws

interface ISimManager {
    /**
     * Obtiene la linea predeterminada para llamadas. Si la versión de
     * las apis android es 24 o mayor obtendrá la predeterminada del sistema.
     * De lo contrario obtendrá la que se estableció como predeterminada manualmente
     * mediante el método [setDefaultVoiceSim]. Si es android 23 o 22 y no existe ninguna
     * linea establecida como predeterminada se lanza un [IllegalStateException].
     *
     * @param withRelations - Indica si se debe obtener la linea con sus relaciones foráneas.
     *
     * @return [Sim]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getDefaultVoiceSim(withRelations: Boolean = false): Sim

    /**
     * Establece la linea predeterminada para las llamadas. Esto se usa
     * solamente en las apis 23 y 22 ya que no se pueden obtener
     * mediante el sistema.
     *
     * @param sim - Linea que se establecerá como predeterminada.
     * */
    suspend fun setDefaultVoiceSim(sim: Sim)

    /**
     * Obtiene la linea predeterminada para los datos. Si la versión de
     * las apis android es 24 o mayor obtendrá la predeterminada del sistema.
     * De lo contrario obtendrá la que se estableció como predeterminada manualmente
     * mediante el método [setDefaultDataSim]. Si es android 23 o 22 y no existe ninguna
     * linea establecida como predeterminada se lanza un [IllegalStateException].
     *
     * @param withRelations - Indica si se debe obtener la linea con sus relaciones foráneas.
     *
     * @return [Sim]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getDefaultDataSim(withRelations: Boolean = false): Sim

    /**
     * Establece la linea predeterminada para los datos. Esto se usa
     * solamente en las apis 23 y 22 ya que no se pueden obtener
     * mediante el sistema.
     *
     * @param sim - Linea que se establecerá como predeterminada.
     * */
    suspend fun setDefaultDataSim(sim: Sim)

    /**
     * Indica si hay más de una linea instalada.
     * */
    @Throws(MissingPermissionException::class)
    suspend fun isInstalledSeveralSims(): Boolean

    /**
     * Obtiene todas las lineas instaladas.
     *
     * @return [List]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getInstalledSims(withRelations: Boolean = false): List<Sim>

    @Throws(MissingPermissionException::class)
    fun flowInstalledSims(withRelations: Boolean): Flow<List<Sim>>

    /**
     * Obtiene una linea por el índice.
     *
     * @param simIndex - Índice en base a 1 de la linea. Normalmente este es el
     * slot donde está instalada.
     * @param withRelations - Indica si se debe obtener la linea con sus relaciones foráneas.
     *
     * @return [Sim]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getSimByIndex(simIndex: Int, withRelations: Boolean = false): Sim
}