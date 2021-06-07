package com.smartsolutions.paquetes.repositories.contracts

import com.smartsolutions.paquetes.repositories.models.Sim
import kotlinx.coroutines.flow.Flow

interface ISimRepository {

    suspend fun create(sim: Sim)

    suspend fun create(sims: List<Sim>)

    suspend fun all(withRelations: Boolean = false): List<Sim>

    fun flow(withRelations: Boolean = false): Flow<List<Sim>>

    suspend fun get(id: String, withRelations: Boolean = false): Sim?

    suspend fun update(sim: Sim): Int

    suspend fun update(sims: List<Sim>): Int

    suspend fun delete(sim: Sim): Int
}