package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager

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
        Index(
            "data_package_id"
        )
])
@TypeConverters(PurchasedPackage.BuyModeConverter::class)
data class PurchasedPackage(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val date: Long,
    val origin: IDataPackageManager.BuyMode,
    @ColumnInfo(name = "sim_id")
    var simId: String,
    var pending: Boolean,
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: String
) {

    @Ignore
    lateinit var dataPackage: DataPackage

    @Ignore
    lateinit var sim: Sim

    class BuyModeConverter {

        @TypeConverter
        fun fromOrigin(origin: IDataPackageManager.BuyMode): String =
            origin.name

        @TypeConverter
        fun toOrigin(name: String): IDataPackageManager.BuyMode =
            IDataPackageManager.BuyMode.valueOf(name)
    }
}