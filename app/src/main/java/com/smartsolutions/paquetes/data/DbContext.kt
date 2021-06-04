package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
    SimDataPackage::class], version = 1, exportSchema = false)
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

    /**
     * @return Data Access Object para consultar la tabla pivote sims_data_packages.
     * */
    abstract fun getSimDataPackageDao(): ISimDataPackageDao

    /**
     * Seeder de la tabla data_packages
     * */
    internal class DataPackageDbSeeder: RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let { dbContext ->
                val dao = dbContext.getDataPackageDao()

                /*val dataPackages = listOf(
                    //Bolsas
                    DataPackage(
                        DailyBag.id,
                        DailyBag.name,
                        DailyBag.description,
                        DailyBag.price,
                        DailyBag.bytes.value.toLong(),
                        convertToBytes(DailyBag.bytesLte),
                        DailyBag.bonusBytes.value.toLong(),
                        DailyBag.bonusCuBytes.value.toLong(),
                        DailyBag.network,
                        DailyBag.index
                    ),
                    //Paquetes LTE
                    DataPackage(
                        P_1GbLte.id,
                        P_1GbLte.name,
                        P_1GbLte.description,
                        P_1GbLte.price,
                        P_1GbLte.bytes.value.toLong(),
                        convertToBytes(P_1GbLte.bytesLte),
                        P_1GbLte.bonusBytes.value.toLong(),
                        convertToBytes(P_1GbLte.bonusCuBytes),
                        P_1GbLte.network,
                        P_1GbLte.index
                    ),
                    DataPackage(
                        P_2_5GbLte.id,
                        P_2_5GbLte.name,
                        P_2_5GbLte.description,
                        P_2_5GbLte.price,
                        0,
                        convertToBytes(P_2_5GbLte.bytesLte),
                        P_2_5GbLte.bonusBytes.value.toLong(),
                        convertToBytes(P_2_5GbLte.bonusCuBytes),
                        P_2_5GbLte.network,
                        P_2_5GbLte.index
                    ),
                    DataPackage(
                        P_14GbLte.id,
                        P_14GbLte.name,
                        P_14GbLte.description,
                        P_14GbLte.price,
                        convertToBytes(P_14GbLte.bytes),
                        convertToBytes(P_14GbLte.bytesLte),
                        P_14GbLte.bonusBytes.value.toLong(),
                        convertToBytes(P_14GbLte.bonusCuBytes),
                        P_14GbLte.network,
                        P_14GbLte.index
                    ),
                    //Paquetes
                    DataPackage(
                        P_400Mb.id,
                        P_400Mb.name,
                        P_400Mb.description,
                        P_400Mb.price,
                        convertToBytes(P_400Mb.bytes),
                        P_400Mb.bytesLte.value.toLong(),
                        convertToBytes(P_400Mb.bonusBytes),
                        convertToBytes(P_400Mb.bonusCuBytes),
                        P_400Mb.network,
                        P_400Mb.index
                    ),
                    DataPackage(
                        P_600Mb.id,
                        P_600Mb.name,
                        P_600Mb.description,
                        P_600Mb.price,
                        convertToBytes(P_600Mb.bytes),
                        P_600Mb.bytesLte.value.toLong(),
                        convertToBytes(P_600Mb.bonusBytes),
                        convertToBytes(P_600Mb.bonusCuBytes),
                        P_600Mb.network,
                        P_600Mb.index
                    ),
                    DataPackage(
                        P_1Gb.id,
                        P_1Gb.name,
                        P_1Gb.description,
                        P_1Gb.price,
                        convertToBytes(P_1Gb.bytes),
                        P_1Gb.bytesLte.value.toLong(),
                        convertToBytes(P_1Gb.bonusBytes),
                        convertToBytes(P_1Gb.bonusCuBytes),
                        P_1Gb.network,
                        P_1Gb.index
                    ),
                    DataPackage(
                        P_2_5Gb.id,
                        P_2_5Gb.name,
                        P_2_5Gb.description,
                        P_2_5Gb.price,
                        convertToBytes(P_2_5Gb.bytes),
                        P_2_5Gb.bytesLte.value.toLong(),
                        convertToBytes(P_2_5Gb.bonusBytes),
                        convertToBytes(P_2_5Gb.bonusCuBytes),
                        P_2_5Gb.network,
                        P_2_5Gb.index
                    ),
                    DataPackage(
                        P_4Gb.id,
                        P_4Gb.name,
                        P_4Gb.description,
                        P_4Gb.price,
                        convertToBytes(P_4Gb.bytes),
                        P_4Gb.bytesLte.value.toLong(),
                        convertToBytes(P_4Gb.bonusBytes),
                        convertToBytes(P_4Gb.bonusCuBytes),
                        P_4Gb.network,
                        P_4Gb.index
                    )
                )

                GlobalScope.launch(Dispatchers.Main) {
                    dao.create(dataPackages)
                }*/

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
                            packageModel.index
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