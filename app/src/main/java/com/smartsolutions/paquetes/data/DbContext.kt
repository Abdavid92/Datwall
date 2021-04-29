package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsolutions.paquetes.helpers.convertToBytes
import com.smartsolutions.paquetes.helpers.createDataPackageId
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import com.smartsolutions.paquetes.data.DataPackagesContract.DailyBag
import com.smartsolutions.paquetes.data.DataPackagesContract.P_1GbLte
import com.smartsolutions.paquetes.data.DataPackagesContract.P_2_5GbLte
import com.smartsolutions.paquetes.data.DataPackagesContract.P_14GbLte
import com.smartsolutions.paquetes.data.DataPackagesContract.P_400Mb
import com.smartsolutions.paquetes.data.DataPackagesContract.P_600Mb
import com.smartsolutions.paquetes.data.DataPackagesContract.P_1Gb
import com.smartsolutions.paquetes.data.DataPackagesContract.P_2_5Gb
import com.smartsolutions.paquetes.data.DataPackagesContract.P_4Gb

/**
 * Conexión de la base de datos de las aplicaciones y los paquetes.
 * */
@Database(entities = [
    App::class,
    DataPackage::class,
    PurchasedPackage::class], version = 1, exportSchema = false)
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
     * Seeder de la tabla data_packages
     * */
    internal class DataPackageDbSeeder: RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            INSTANCE?.let {
                val dao = it.getDataPackageDao()

                val dataPackages = listOf(
                    //Bolsas
                    DataPackage(
                        createDataPackageId(DailyBag.name, DailyBag.price),
                        DailyBag.name,
                        DailyBag.description,
                        DailyBag.price,
                        DailyBag.bytes,
                        convertToBytes(DailyBag.bytesLte),
                        DailyBag.bonusBytes,
                        DailyBag.bonusCuBytes,
                        DailyBag.network,
                        "",
                        DailyBag.index
                    ),
                    //Paquetes LTE
                    DataPackage(
                        createDataPackageId(P_1GbLte.name, P_1GbLte.price),
                        P_1GbLte.name,
                        P_1GbLte.description,
                        P_1GbLte.price,
                        P_1GbLte.bytes,
                        convertToBytes(P_1GbLte.bytesLte),
                        P_1GbLte.bonusBytes,
                        convertToBytes(P_1GbLte.bonusCuBytes),
                        P_1GbLte.network,
                        "",
                        P_1GbLte.index
                    ),
                    DataPackage(
                        createDataPackageId(P_2_5GbLte.name, P_2_5GbLte.price),
                        P_2_5GbLte.name,
                        P_2_5GbLte.description,
                        P_2_5GbLte.price,
                        0,
                        convertToBytes(P_2_5GbLte.bytesLte),
                        P_2_5GbLte.bonusBytes,
                        convertToBytes(P_2_5GbLte.bonusCuBytes),
                        P_2_5GbLte.network,
                        "",
                        P_2_5GbLte.index
                    ),
                    DataPackage(
                        createDataPackageId(P_14GbLte.name, P_14GbLte.price),
                        P_14GbLte.name,
                        P_14GbLte.description,
                        P_14GbLte.price,
                        convertToBytes(P_14GbLte.bytes),
                        convertToBytes(P_14GbLte.bytesLte),
                        P_14GbLte.bonusBytes,
                        convertToBytes(P_14GbLte.bonusCuBytes),
                        P_14GbLte.network,
                        "",
                        P_14GbLte.index
                    ),
                    //Paquetes
                    DataPackage(
                        createDataPackageId(P_400Mb.name, P_400Mb.price),
                        P_400Mb.name,
                        P_400Mb.description,
                        P_400Mb.price,
                        convertToBytes(P_400Mb.bytes),
                        P_400Mb.bytesLte,
                        convertToBytes(P_400Mb.bonusBytes),
                        convertToBytes(P_400Mb.bonusCuBytes),
                        P_400Mb.network,
                        "",
                        P_400Mb.index
                    ),
                    DataPackage(
                        createDataPackageId(P_600Mb.name, P_600Mb.price),
                        P_600Mb.name,
                        P_600Mb.description,
                        P_600Mb.price,
                        convertToBytes(P_600Mb.bytes),
                        P_600Mb.bytesLte,
                        convertToBytes(P_600Mb.bonusBytes),
                        convertToBytes(P_600Mb.bonusCuBytes),
                        P_600Mb.network,
                        "",
                        P_600Mb.index
                    ),
                    DataPackage(
                        createDataPackageId(P_1Gb.name, P_1Gb.price),
                        P_1Gb.name,
                        P_1Gb.description,
                        P_1Gb.price,
                        convertToBytes(P_1Gb.bytes),
                        0,
                        convertToBytes(P_1Gb.bonusBytes),
                        convertToBytes(P_1Gb.bonusCuBytes),
                        P_1Gb.network,
                        "",
                        P_1Gb.index
                    ),
                    DataPackage(
                        createDataPackageId(P_2_5Gb.name, P_2_5Gb.price),
                        P_2_5Gb.name,
                        P_2_5Gb.description,
                        P_2_5Gb.price,
                        convertToBytes(P_2_5Gb.bytes),
                        P_2_5Gb.bytesLte,
                        convertToBytes(P_2_5Gb.bonusBytes),
                        convertToBytes(P_2_5Gb.bonusCuBytes),
                        P_2_5Gb.network,
                        "",
                        P_2_5Gb.index
                    ),
                    DataPackage(
                        createDataPackageId(P_4Gb.name, P_4Gb.price),
                        P_4Gb.name,
                        P_4Gb.description,
                        P_4Gb.price,
                        convertToBytes(P_4Gb.bytes),
                        P_4Gb.bytesLte,
                        convertToBytes(P_4Gb.bonusBytes),
                        convertToBytes(P_4Gb.bonusCuBytes),
                        P_4Gb.network,
                        "",
                        P_4Gb.index
                    ),
                )

                GlobalScope.launch(Dispatchers.IO) {
                    dao.create(dataPackages)
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