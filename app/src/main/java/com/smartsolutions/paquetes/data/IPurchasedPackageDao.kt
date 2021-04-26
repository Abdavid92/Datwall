package com.smartsolutions.paquetes.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow

@Dao
interface IPurchasedPackageDao {

    @Query("SELECT * FROM purchased_packages ORDER BY id DESC")
    fun getAll(): Flow<List<PurchasedPackage>>

    @Query("SELECT * FROM purchased_packages WHERE date >= :start AND date <= :finish ORDER BY id DESC")
    fun getByDate(start: Long, finish: Long): Flow<List<PurchasedPackage>>

    @Query("SELECT * FROM purchased_packages WHERE id = :id")
    fun get(id: Long): Flow<PurchasedPackage>

    @Query("SELECT * FROM purchased_packages WHERE data_package_id = :dataPackageId ORDER BY id DESC")
    fun getByDataPackageId(dataPackageId: String): Flow<List<PurchasedPackage>>

    @Insert
    suspend fun create(purchasedPackage: PurchasedPackage): Long

    @Insert
    suspend fun create(purchasedPackages: List<PurchasedPackage>): List<Long>

    @Update
    suspend fun update(purchasedPackage: PurchasedPackage): Int

    @Update
    suspend fun update(purchasedPackages: List<PurchasedPackage>): Int

    @Delete
    suspend fun delete(purchasedPackage: PurchasedPackage): Int

    @Delete
    suspend fun delete(purchasedPackages: List<PurchasedPackage>): Int
}