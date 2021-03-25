package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.App

@Dao
interface IAppDao {

    @get:Query("SELECT count() FROM apps")
    val appsCount: Int

    @get:Query("SELECT count() FROM apps WHERE access = 1")
    val appsAllowedCount: Int

    @get:Query("SELECT count() FROM apps WHERE access = 0")
    val appsBlockedCount: Int

    @get:Query("SELECT * FROM apps")
    val apps: List<App>

    @Query("SELECT * FROM apps WHERE package_name = :packageName")
    fun getApp(packageName: String): App

    @Insert
    suspend fun create(app: App)

    @Update
    suspend fun update(app: App)

    @Update
    suspend fun update(apps: List<App>)

    @Delete
    suspend fun delete(app: App)
}