package com.smartsolutions.paquetes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartsolutions.paquetes.helpers.DataUnit.*
import com.smartsolutions.paquetes.helpers.DataValue
import com.smartsolutions.paquetes.helpers.convertToBytes
import com.smartsolutions.paquetes.helpers.createDataPackageId
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.DataPackage.Companion.NETWORK_3G_4G
import com.smartsolutions.paquetes.repositories.models.DataPackage.Companion.NETWORK_4G
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

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
                        createDataPackageId("Bolsa Diaria LTE", 25f),
                        "Bolsa Diaria LTE",
                        "Navega en la red LTE por 24 horas.",
                        25f,
                        0,
                        convertToBytes(DataValue(200.0, MB)),
                        0,
                        0,
                        NETWORK_4G,
                        ""
                    ),
                    //Paquetes LTE
                    DataPackage(
                        createDataPackageId("Paquete 1 GB LTE", 100f),
                        "Paquete 1 GB LTE",
                        "Este paquete consta de 1 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias.",
                        100f,
                        0,
                        convertToBytes(DataValue(1.0, GB)),
                        0,
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_4G,
                        ""
                    ),
                    DataPackage(
                        createDataPackageId("Paquete 2.5 GB LTE", 200f),
                        "Paquete 2.5 GB LTE",
                        "Este paquete consta de 2.5 GB que solo podrá utilizar bajo la red 4G(LTE). Tiene un vigencia de 30 dias.",
                        200f,
                        0,
                        convertToBytes(DataValue(2.5, GB)),
                        0,
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_4G,
                        ""
                    ),
                    DataPackage(
                        createDataPackageId("Paquete 14 GB", 1125f),
                        "Paquete 14 GB",
                        "Este paquete consta de 10 GB que solo podrá utilizar bajo la red 4G(LTE) y " +
                                "4 GB que podrá usar en todas las redes. Tiene un vigencia de 30 dias.",
                        1125f,
                        convertToBytes(DataValue(4.0, GB)),
                        convertToBytes(DataValue(10.0, GB)),
                        0,
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_4G,
                        ""
                    ),
                    //Paquetes
                    DataPackage(
                        createDataPackageId("Paquete 400 MB", 125f),
                        "Paquete 400 MB",
                        "Este paquete consta de 400 MB que podrá usar en todas las redes y" +
                                " un bono de 500 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
                        125f,
                        convertToBytes(DataValue(400.0, MB)),
                        0,
                        convertToBytes(DataValue(500.0, MB)),
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_3G_4G,
                        ""
                    ),
                    DataPackage(
                        createDataPackageId("Paquete 600 MB", 175f),
                        "Paquete 600 MB",
                        "Este paquete consta de 600 MB que podrá usar en todas las redes y" +
                                " un bono de 800 MB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
                        175f,
                        convertToBytes(DataValue(600.0, MB)),
                        0,
                        convertToBytes(DataValue(800.0, MB)),
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_3G_4G,
                        ""
                    ),
                    DataPackage(
                        createDataPackageId("Paquete 1 GB", 250f),
                        "Paquete 1 GB",
                        "Este paquete consta de 1 GB que podrá usar en todas las redes y" +
                                " un bono de 1.5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
                        250f,
                        convertToBytes(DataValue(1.0, GB)),
                        0,
                        convertToBytes(DataValue(1.5, GB)),
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_3G_4G,
                        ""
                    ),
                    DataPackage(
                        createDataPackageId("Paquete 2.5 GB", 500f),
                        "Paquete 2.5 GB",
                        "Este paquete consta de 2.5 GB que podrá usar en todas las redes y" +
                                " un bono de 3 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
                        500f,
                        convertToBytes(DataValue(2.5, GB)),
                        0,
                        convertToBytes(DataValue(3.0, GB)),
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_3G_4G,
                        ""
                    ),
                    DataPackage(
                        createDataPackageId("Paquete 4 GB", 750f),
                        "Paquete 4 GB",
                        "Este paquete consta de 4 GB que podrá usar en todas las redes y" +
                                " un bono de 5 GB que solo podrá utilizar en bajo la red 4G(LTE). Tiene una vigencia de 30 dias.",
                        750f,
                        convertToBytes(DataValue(4.0, GB)),
                        0,
                        convertToBytes(DataValue(5.0, GB)),
                        convertToBytes(DataValue(300.0, MB)),
                        NETWORK_3G_4G,
                        ""
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