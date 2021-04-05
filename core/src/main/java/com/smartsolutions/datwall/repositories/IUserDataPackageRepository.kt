package com.smartsolutions.datwall.repositories

import androidx.lifecycle.LiveData
import com.smartsolutions.datwall.repositories.models.UserDataPackage

interface IUserDataPackageRepository {

    fun getAll(): LiveData<List<UserDataPackage>>

    suspend fun get(id: Long): UserDataPackage

    suspend fun create(userDataPackage: UserDataPackage): Long

    suspend fun create(userDataPackages: List<UserDataPackage>): List<Long>

    suspend fun update(userDataPackage: UserDataPackage): Int

    suspend fun update(userDataPackages: List<UserDataPackage>): Int

    suspend fun delete(userDataPackage: UserDataPackage): Int

    suspend fun delete(userDataPackages: List<UserDataPackage>): Int
}