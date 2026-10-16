package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.flow.Flow

/**
 * Datos de cada línea (megas disponibles por tipo) y su consumo, para la UI.
 * */
interface IUserDataManager {

    fun flowUserData(simId: String): Flow<List<UserDataBytes>>

    suspend fun get(simId: String, type: DataBytes.DataType): UserDataBytes

    /**
     * Guarda una edición manual ya validada (ver `UserDataBytesEditor`).
     * */
    suspend fun save(userDataBytes: UserDataBytes)

    /**
     * Consumo registrado de [type] en [simId] entre [start] y [finish].
     * */
    suspend fun usage(simId: String, type: DataBytes.DataType, start: Long, finish: Long): List<UsageGeneral>
}
