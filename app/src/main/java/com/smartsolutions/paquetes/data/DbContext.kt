package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsolutions.paquetes.managers.models.Traffic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.smartsolutions.paquetes.repositories.models.*

/**
 * Conexión de la base de datos de las aplicaciones y los paquetes.
 * */
@Database(entities = [
    App::class,
    DataPackage::class,
    PurchasedPackage::class,
    Sim::class,
    MiCubacelAccount::class,
    UserDataBytes::class,
    Traffic::class], version = 1, exportSchema = false)
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
     * @return Data Access Object para consultar la tabla purchased_packages.
     * */
    abstract fun getPurchasedPackageDao(): IPurchasedPackageDao

    /**
     * @return Data Access Object para consultar la tabla sims.
     * */
    abstract fun getSimDao(): ISimDao

    abstract fun getMiCubacelAccountDao(): IMiCubacelAccountDao

    abstract fun getUserDataBytesDao(): IUserDataBytesDao

    abstract fun getTrafficDao(): ITrafficDao

    /**
     * Seeder de la tabla data_packages
     * */
    internal class DataPackageDbSeeder: RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let { dbContext ->
                val dao = dbContext.getDataPackageDao()

                GlobalScope.launch(Dispatchers.Main) {
                    DataPackagesContract.PackagesList.forEach { packageModel ->

                        val dataPackage = DataPackage(
                            packageModel.id,
                            packageModel.name,
                            packageModel.description,
                            packageModel.price,
                            packageModel.bytes.toBytes(),
                            packageModel.bytesLte.toBytes(),
                            packageModel.bonusBytes.toBytes(),
                            packageModel.bonusCuBytes.toBytes(),
                            packageModel.network,
                            packageModel.index,
                            packageModel.duration
                        )

                        dao.create(dataPackage)
                    }
                }
            }
        }
    }

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
                    "data.db")
                    .addCallback(DataPackageDbSeeder())
                    //.addMigrations()
                    .build()

                return INSTANCE!!
            }
        }
    }
}