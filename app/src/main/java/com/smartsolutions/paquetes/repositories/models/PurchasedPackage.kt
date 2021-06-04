package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import com.smartsolutions.paquetes.managers.IDataPackageManager

@Entity(tableName = "purchased_packages",
    foreignKeys = [
        ForeignKey(
            entity = DataPackage::class,
            parentColumns = ["id"],
            childColumns = ["data_package_id"]
)], indices = [
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
    @ColumnInfo(name = "sim_index")
    var simIndex: Int,
    var pending: Boolean,
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: String
) {

    @Ignore
    var dataPackage: DataPackage? = null

    class BuyModeConverter {

        @TypeConverter
        fun fromOrigin(origin: IDataPackageManager.BuyMode): String =
            origin.name

        @TypeConverter
        fun toOrigin(name: String): IDataPackageManager.BuyMode =
            IDataPackageManager.BuyMode.valueOf(name)
    }
}