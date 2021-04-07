package com.smartsolutions.datwall.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsolutions.datwall.managers.models.Traffic

@Database(entities = [Traffic::class], version = 1, exportSchema = false)
abstract class TrafficDbContext : RoomDatabase() {

    companion object{
        fun getInstance(context: Context) = Room.databaseBuilder(context, TrafficDbContext::class.java, "traffic.db").build()
    }

    abstract fun getTrafficDao() : ITrafficDao

}