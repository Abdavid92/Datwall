package com.smartsolutions.paquetes.repositories.models

import androidx.room.*

@Entity(tableName = "purchased_packages", foreignKeys = [ForeignKey(
    entity = DataPackage::class, parentColumns = ["id"], childColumns = ["data_package_id"]
)], indices = [Index(
    "data_package_id"
)])
@TypeConverters(PurchasedPackage.OriginConverter::class)
data class PurchasedPackage(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val date: Long,
    val origin: Origin,
    @ColumnInfo(name = "data_package_id")
    val dataPackageId: String
) {

    @Ignore
    var dataPackage: DataPackage? = null

    enum class Origin {
        USSD,
        MICUBACEL
    }

    class OriginConverter {

        @TypeConverter
        fun fromOrigin(origin: Origin): String =
            origin.name

        @TypeConverter
        fun toOrigin(name: String): Origin =
            Origin.valueOf(name)
    }
}