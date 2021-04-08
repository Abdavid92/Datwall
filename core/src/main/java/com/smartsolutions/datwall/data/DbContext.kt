package com.smartsolutions.datwall.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.DataPackage
import com.smartsolutions.datwall.repositories.models.UserDataPackage

/**
 * Conexión de la base de datos de las aplicaciones y los paquetes.
 * */
@Database(entities = [App::class, DataPackage::class, UserDataPackage::class], version = 1, exportSchema = false)
abstract class DbContext: RoomDatabase() {

    /**
     * @return Data Access Object para consultar la tabla apps.
     * */
    abstract fun getAppDao(): IAppDao

    /**
     * @return Data Access Object para consultar la tabla data_packages.
     * */
    abstract fun getDataPackageDao(): IDataPackageDao

    /**
     * @return Data Access Object para consultar la tabla user_data_packages.
     * */
    abstract fun getUserDataPackageDao(): IUserDataPackageDao

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