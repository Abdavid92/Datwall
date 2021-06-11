package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager

/**
 * Representa una compra de un paquete.
 * */
@Entity(tableName = "purchased_packages",
    foreignKeys = [
        ForeignKey(
            entity = DataPackage::class,
            parentColumns = ["id"],
            childColumns = ["data_package_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Sim::class,
            parentColumns = ["id"],
            childColumns = ["sim_id"],
            onDelete = ForeignKey.CASCADE
        )
],
    indices = [
        Index("data_package_id"),
        Index("sim_id")
])
@TypeConverters(PurchasedPackage.BuyModeConverter::class)
data class PurchasedPackage(
    /**
     * Id de la compra.
     * */
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    /**
     * Fecha en que se realizó la compra.
     * */
    val date: Long,
    /**
     * Origen de la compra (USSD o mi.cubacel.net).
     * */
    val origin: IDataPackageManager.ConnectionMode,
    /**
     * Linea por donde se efectuó la compra.
     * */
    @ColumnInfo(name = "sim_id")
    var simId: String,
    /**
     * Indica si la compra está pendiente a confirmar.
     * */
    var pending: Boolean,
    /**
     * Id del paquete que se compró.
     * */
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: String
) {

    /**
     * Paquete que se compró.
     * */
    @Ignore
    lateinit var dataPackage: DataPackage

    /**
     * Linea por donde se efectuó la compra.
     * */
    @Ignore
    lateinit var sim: Sim

    class BuyModeConverter {

        @TypeConverter
        fun fromOrigin(origin: IDataPackageManager.ConnectionMode): String =
            origin.name

        @TypeConverter
        fun toOrigin(name: String): IDataPackageManager.ConnectionMode =
            IDataPackageManager.ConnectionMode.valueOf(name)
    }
}