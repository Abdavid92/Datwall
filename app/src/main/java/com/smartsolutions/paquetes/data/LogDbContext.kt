package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsolutions.paquetes.repositories.models.Event

@Database(
    entities = [
        Event::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LogDbContext : RoomDatabase() {

    abstract fun getIEventDao(): IEventDao

    companion object {

        @Volatile
        private var INSTANCE: LogDbContext? = null

        fun getInstance(context: Context): LogDbContext {

            var instance = INSTANCE

            if (instance == null) {
                synchronized(this) {
                    instance = Room.databaseBuilder(
                        context,
                        LogDbContext::class.java,
                        "logs.db"
                    ).build()

                    INSTANCE = instance
                }
            }

            return instance!!
        }
    }
}