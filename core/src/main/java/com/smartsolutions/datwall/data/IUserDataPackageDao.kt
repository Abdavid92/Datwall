package com.smartsolutions.datwall.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smartsolutions.datwall.repositories.models.UserDataPackage

@Dao
interface IUserDataPackageDao {

    @Query("SELECT * FROM user_data_package")
    fun getAll(): LiveData<List<UserDataPackage>>

    @Query("SELECT * FROM user_data_package WHERE id = :id")
    suspend fun get(id: Long): UserDataPackage

    @Insert
    suspend fun create(userDataPackage: UserDataPackage): Long

    @Insert
    suspend fun create(userDataPackages: List<UserDataPackage>): List<Long>

    @Update
    suspend fun update(userDataPackage: UserDataPackage): Int

    @Update
    suspend fun update(userDataPackages: List<UserDataPackage>): Int

    @Delete
    suspend fun delete(userDataPackage: UserDataPackage): Int

    @Delete
    suspend fun delete(userDataPackages: List<UserDataPackage>): Int
}