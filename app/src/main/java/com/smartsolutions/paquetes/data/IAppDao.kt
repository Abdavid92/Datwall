package com.smartsolutions.paquetes.data

import androidx.room.*
import com.smartsolutions.paquetes.repositories.models.App
import kotlinx.coroutines.flow.Flow

@Dao
interface IAppDao {

    @Query("SELECT count() FROM apps")
    suspend fun appsCount(): Int

    @Query("SELECT count() FROM apps WHERE access = 1")
    suspend fun appsAllowedCount(): Int

    @Query("SELECT count() FROM apps WHERE access = 0")
    suspend fun appsBlockedCount(): Int

    @Query("SELECT * FROM apps ORDER BY name")
    suspend fun apps(): List<App>

    @Query("SELECT * FROM apps ORDER BY uid")
    fun flow(): Flow<List<App>>

    @Query("SELECT * FROM apps WHERE package_name = :packageName")
    fun get(packageName: String): App?

    @Query("SELECT * FROM apps WHERE uid = :uid")
    fun get(uid: Int): List<App>

    @Query("SELECT * FROM apps WHERE uid IN (:uid)")
    fun get(uid: IntArray): List<App>

    @Insert
    suspend fun create(app: App)

    @Insert
    suspend fun create(apps: List<App>)

    @Update
    suspend fun update(app: App): Int

    @Update
    suspend fun update(apps: List<App>): Int

    @Delete
    suspend fun delete(app: App): Int

    @Delete
    suspend fun delete(apps: List<App>): Int
}