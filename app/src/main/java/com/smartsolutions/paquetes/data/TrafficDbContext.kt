package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.DataPackage

/**
 * Conexión al la base de datos traffic.db
 * */
@Database(entities = [Traffic::class], version = 1, exportSchema = false)
abstract class TrafficDbContext : RoomDatabase() {

    internal class TrafficDbSeeder: RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let {
                val dao = it.getTrafficDao()

                val dataPackages = listOf(
                    //Bolsas
                    DataPackage(
                        0,
                        "Bolsa Correo",
                        "La Bolsa de Correo te permite acceder a tu cuenta de correo Nauta en tu móvil, desde donde estés.",
                        25,
                        100,//TODO: Esto está mal
                        0,
                        0,
                        DataPackage.NETWORK_3G_4G,
                        "",//TODO: Falta esto
                        true
                    ),
                    DataPackage(
                        0,
                        "Bolsa Diaria LTE",
                        "Navega en la red LTE por 24 horas.",
                        25,
                        200,
                        0,
                        0,
                        DataPackage.NETWORK_4G,
                        "",
                        true
                    ),
                    //Paquetes LTE
                    DataPackage(
                        0,
                        "Paquete 1 GB LTE",
                        "Navega en la red LTE por 30 días.",
                        100,
                        1,//TODO:Esto está mal
                    0,
                        300,
                        DataPackage.NETWORK_4G,
                        "",
                        true
                    ),
                    DataPackage(
                        0,
                        "Paquete 2.5 GB LTE",
                        "Navega en la red LTE por 30 días.",
                        200,
                        2,
                        0,
                        300,
                        DataPackage.NETWORK_4G,
                        "",
                        true
                    ),
                    DataPackage(
                        0,
                        "Paquete 14 GB",
                        "Navega en la red LTE por 30 días.",
                        1125,
                        14,
                        0,
                        300,
                        DataPackage.NETWORK_4G,
                        "",
                        true
                    ),
                    //Paquetes
                    DataPackage(
                        0,
                        "Paquete 400 MB",
                        "",
                        125,
                        400,
                        500,
                        300,
                        DataPackage.NETWORK_3G_4G,
                        "",
                        true
                    ),
                    DataPackage(
                        0,
                        "Paquete 600 MB",
                        "",
                        175,
                        600,
                        0,
                        300,
                        DataPackage.NETWORK_4G,
                        "",
                        true
                    )
                )
            }
        }
    }

    companion object {

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
                    .addCallback(TrafficDbSeeder())
                    //.addMigrations()
                    .build()

                return INSTANCE!!
            }
        }
    }

    abstract fun getTrafficDao() : ITrafficDao

}