package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.repositories.models.DataPackage
import kotlinx.coroutines.flow.Flow

@Dao
@TypeConverters(DataPackage.PackageIdConverter::class)
interface IDataPackageDao {

    @Query("SELECT * FROM data_packages")
    suspend fun all(): List<DataPackage>

    @Query("SELECT * FROM data_packages")
    fun flow(): Flow<List<DataPackage>>

    @Query("SELECT * FROM data_packages WHERE id = :id")
    suspend fun get(id: DataPackages.PackageId): DataPackage?

    @Query("SELECT * FROM data_packages WHERE network = :network")
    suspend fun getByNetwork(@Networks network: String) : List<DataPackage>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(dataPackage: DataPackage): Long

    @Insert
    suspend fun create(dataPackages: List<DataPackage>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun createOrUpdate(dataPackages: List<DataPackage>)

    @Update
    suspend fun update(dataPackage: DataPackage): Int

    @Update
    suspend fun update(dataPackages: List<DataPackage>): Int

    @Delete
    suspend fun delete(dataPackage: DataPackage): Int

    @Delete
    suspend fun delete(dataPackages: List<DataPackage>): Int
}