package com.smartsolutions.paquetes.repositories

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.repositories.models.DataPackage

interface IDataPackageRepository {

    fun getAll(): LiveData<List<DataPackage>>

    suspend fun get(id: Int): DataPackage?

    suspend fun create(dataPackage: DataPackage): Long

    suspend fun create(dataPackages: List<DataPackage>): List<Long>

    suspend fun update(dataPackage: DataPackage): Int

    suspend fun update(dataPackages: List<DataPackage>): Int

    suspend fun delete(dataPackage: DataPackage): Int

    suspend fun delete(dataPackages: List<DataPackage>): Int
}