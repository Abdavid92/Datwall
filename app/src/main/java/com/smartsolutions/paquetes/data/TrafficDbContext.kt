package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsolutions.paquetes.managers.models.Traffic

/**
 * Conexión al la base de datos traffic.db
 * */
@Database(entities = [Traffic::class], version = 1, exportSchema = false)
abstract class TrafficDbContext : RoomDatabase() {

    companion object{

        @Volatile
        private var INSTANCE: TrafficDbContext? = null

        fun getInstance(context: Context): TrafficDbContext {
            INSTANCE?.let {
                return it
            }

            synchronized(this) {
                INSTANCE = Room.databaseBuilder(
                    context,
                    TrafficDbContext::class.java,
                    "traffic.db")
                    //.addMigrations()
                    .build()

                return INSTANCE!!
            }
        }
    }

    abstract fun getTrafficDao() : ITrafficDao

}