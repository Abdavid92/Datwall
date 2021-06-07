package com.smartsolutions.paquetes.repositories.contracts

import androidx.room.Query
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow

interface IPurchasedPackageRepository {

    fun getAll(): Flow<List<PurchasedPackage>>

    fun getByDate(start: Long, finish: Long): Flow<List<PurchasedPackage>>

    suspend fun getBySimId(simId: String): List<PurchasedPackage>

    fun flowBySimId(simId: String): Flow<List<PurchasedPackage>>

    fun get(id: Long): Flow<PurchasedPackage>

    fun getByDataPackageId(dataPackageId: String): Flow<List<PurchasedPackage>>

    fun getPending(): Flow<List<PurchasedPackage>>

    fun getPending(dataPackageId: String): Flow<List<PurchasedPackage>>

    suspend fun create(purchasedPackage: PurchasedPackage): Long

    suspend fun create(purchasedPackages: List<PurchasedPackage>): List<Long>

    suspend fun update(purchasedPackage: PurchasedPackage): Int

    suspend fun update(purchasedPackages: List<PurchasedPackage>): Int

    suspend fun delete(purchasedPackage: PurchasedPackage): Int

    suspend fun delete(purchasedPackages: List<PurchasedPackage>): Int
}