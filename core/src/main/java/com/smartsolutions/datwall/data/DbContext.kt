package com.smartsolutions.datwall.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartsolutions.datwall.repositories.models.App

@Database(entities = [App::class], version = 1, exportSchema = false)
abstract class DbContext: RoomDatabase() {

    abstract fun getAppDao(): IAppDao
}