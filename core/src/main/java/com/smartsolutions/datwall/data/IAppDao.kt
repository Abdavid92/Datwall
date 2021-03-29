package com.smartsolutions.datwall.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.smartsolutions.datwall.repositories.models.App

@Dao
interface IAppDao {

    @get:Query("SELECT count() FROM apps")
    val appsCount: Int

    @get:Query("SELECT count() FROM apps WHERE access = 1")
    val appsAllowedCount: Int

    @get:Query("SELECT count() FROM apps WHERE access = 0")
    val appsBlockedCount: Int

    @get:Query("SELECT * FROM apps ORDER BY name")
    val apps: List<App>

    @Query("SELECT * FROM apps ORDER BY uid")
    fun liveData(): LiveData<List<App>>

    @Query("SELECT * FROM apps WHERE package_name = :packageName")
    fun get(packageName: String): App?

    @Query("SELECT * FROM apps WHERE uid = :uid")
    fun get(uid: Int): List<App>

    @Insert
    suspend fun create(app: App)

    @Update
    suspend fun update(app: App)

    @Update
    suspend fun update(apps: List<App>)

    @Delete
    suspend fun delete(app: App)
}