package com.smartsolutions.datwall.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsolutions.datwall.repositories.models.App

@Database(entities = [App::class], version = 1, exportSchema = false)
abstract class DbContext: RoomDatabase() {

    abstract fun getAppDao(): IAppDao

    companion object {

        @Volatile
        private var INSTANCE: DbContext? = null

        fun getInstance(context: Context): DbContext {
            INSTANCE?.let {
                return it
            }

            synchronized(this) {
                //Aquí se configuran la migraciones
                INSTANCE = Room.databaseBuilder(
                    context,
                    DbContext::class.java,
                    "data.db")
                    //.addMigrations()
                    .build()

                return INSTANCE!!
            }
        }
    }
}