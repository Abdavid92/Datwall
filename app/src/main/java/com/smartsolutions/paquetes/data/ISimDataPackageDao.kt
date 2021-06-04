package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.SimDataPackage

@Dao
interface ISimDataPackageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(simDataPackage: SimDataPackage): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun create(simDataPackages: List<SimDataPackage>): Array<Long>

    @Query("SELECT * FROM sims_data_packages")
    suspend fun all(): List<SimDataPackage>

    @Query("SELECT * FROM sims_data_packages WHERE sim_id = :simId")
    suspend fun bySimId(simId: String): List<SimDataPackage>

    @Query("SELECT * FROM sims_data_packages WHERE data_package_id = :dataPackageId")
    suspend fun byDataPackageId(dataPackageId: String): List<SimDataPackage>

    @Query("SELECT * FROM sims_data_packages WHERE id = :id")
    suspend fun get(id: Int): SimDataPackage?

    @Query("SELECT * FROM sims_data_packages WHERE sim_id = :simId AND data_package_id = :dataPackageId")
    suspend fun get(simId: String, dataPackageId: String): SimDataPackage?

    @Update
    suspend fun update(simDataPackage: SimDataPackage): Int

    @Update
    suspend fun update(simDataPackages: List<SimDataPackage>): Int

    @Delete
    suspend fun delete(simDataPackage: SimDataPackage): Int
}