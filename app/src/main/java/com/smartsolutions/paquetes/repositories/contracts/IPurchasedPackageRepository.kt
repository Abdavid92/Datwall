package com.smartsolutions.paquetes.repositories.contracts

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage

interface IPurchasedPackageRepository {
    fun getAll(): LiveData<List<PurchasedPackage>>
    fun getByDate(start: Long, finish: Long): LiveData<List<PurchasedPackage>>
    fun get(id: Long): LiveData<PurchasedPackage>
    fun getByDataPackageId(dataPackageId: String): LiveData<List<PurchasedPackage>>

    suspend fun create(purchasedPackage: PurchasedPackage): Long

    suspend fun create(purchasedPackages: List<PurchasedPackage>): List<Long>

    suspend fun update(purchasedPackage: PurchasedPackage): Int

    suspend fun update(purchasedPackages: List<PurchasedPackage>): Int

    suspend fun delete(purchasedPackage: PurchasedPackage): Int

    suspend fun delete(purchasedPackages: List<PurchasedPackage>): Int
}