package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsolutions.paquetes.managers.models.Traffic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.smartsolutions.paquetes.repositories.models.*
import kotlinx.coroutines.runBlocking

const val DATABASE_VERSION = 1

/**
 * Conexión de la base de datos de las aplicaciones y los paquetes.
 * */
@Database(
    entities = [
        App::class,
        DataPackage::class,
        PurchasedPackage::class,
        Sim::class,
        UserDataBytes::class,
        Traffic::class,
        UsageGeneral::class,
        Event::class], version = DATABASE_VERSION, exportSchema = false
)
abstract class DbContext : RoomDatabase() {

    /**
     * @return Data Access Object para consultar la tabla apps.
     * */
    abstract fun getAppDao(): IAppDao

    /**
     * @return Data Access Object para consultar la tabla data_packages.
     * */
    abstract fun getDataPackageDao(): IDataPackageDao

    /**
     * @return Data Access Object para consultar la tabla purchased_packages.
     * */
    abstract fun getPurchasedPackageDao(): IPurchasedPackageDao

    /**
     * @return: Data Access Object para consultar la tabla de Consumo General
     */
    abstract fun getUsageGeneralDao(): IUsageGeneralDao

    /**
     * @return Data Access Object para consultar la tabla sims.
     * */
    abstract fun getSimDao(): ISimDao

    abstract fun getUserDataBytesDao(): IUserDataBytesDao

    abstract fun getTrafficDao(): ITrafficDao

    abstract fun getEventDao(): IEventDao


    companion object {

        @Volatile
        private var INSTANCE: DbContext? = null

        /**
         * @return Una instancia de DbContext.
         * */
        fun getInstance(context: Context): DbContext {
            INSTANCE?.let {
                return it
            }

            synchronized(this) {
                //Aquí se configuran la migraciones y los seeders
                INSTANCE = Room.databaseBuilder(
                    context,
                    DbContext::class.java,
                    "data.db"
                )
                    .build()

                return INSTANCE!!
            }
        }
    }
}