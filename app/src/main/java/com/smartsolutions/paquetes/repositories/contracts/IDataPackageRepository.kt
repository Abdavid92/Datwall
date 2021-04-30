package com.smartsolutions.paquetes.repositories.contracts

import com.smartsolutions.paquetes.repositories.models.DataPackage
import kotlinx.coroutines.flow.Flow

interface IDataPackageRepository {

    fun getAll(): Flow<List<DataPackage>>

    fun getActives(simIndex: Int): Flow<List<DataPackage>>

    suspend fun get(id: String): DataPackage?

    suspend fun create(dataPackage: DataPackage): Long

    suspend fun create(dataPackages: List<DataPackage>): List<Long>

    suspend fun update(dataPackage: DataPackage): Int

    suspend fun update(dataPackages: List<DataPackage>): Int

    suspend fun delete(dataPackage: DataPackage): Int

    suspend fun delete(dataPackages: List<DataPackage>): Int
}