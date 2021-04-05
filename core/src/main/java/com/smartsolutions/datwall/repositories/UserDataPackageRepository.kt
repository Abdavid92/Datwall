package com.smartsolutions.datwall.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.smartsolutions.datwall.data.IDataPackageDao
import com.smartsolutions.datwall.data.IUserDataPackageDao
import com.smartsolutions.datwall.repositories.models.UserDataPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UserDataPackageRepository @Inject constructor(
    private val userDataPackageDao: IUserDataPackageDao,
    private val dataPackageDao: IDataPackageDao
): IUserDataPackageRepository, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun getAll(): LiveData<List<UserDataPackage>> = userDataPackageDao.getAll().apply {
        Transformations.map(this) {
            launch {
                it.forEach { userDataPackage ->
                    userDataPackage.dataPackage = dataPackageDao.get(userDataPackage.dataPackageId)
                }
            }
        }
    }

    override suspend fun get(id: Long): UserDataPackage = userDataPackageDao.get(id).apply {
        this.dataPackage = dataPackageDao.get(this.dataPackageId)
    }

    override suspend fun create(userDataPackage: UserDataPackage): Long = userDataPackageDao.create(userDataPackage)

    override suspend fun create(userDataPackages: List<UserDataPackage>): List<Long> = userDataPackageDao.create(userDataPackages)

    override suspend fun update(userDataPackage: UserDataPackage): Int = userDataPackageDao.update(userDataPackage)

    override suspend fun update(userDataPackages: List<UserDataPackage>): Int = userDataPackageDao.update(userDataPackages)

    override suspend fun delete(userDataPackage: UserDataPackage): Int = userDataPackageDao.delete(userDataPackage)

    override suspend fun delete(userDataPackages: List<UserDataPackage>): Int = userDataPackageDao.delete(userDataPackages)
}