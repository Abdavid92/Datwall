package com.smartsolutions.paquetes.repositories.models

import androidx.room.*
import java.util.*

/**
 * Representa un tipo de datos con una cantidad determinada a consumir en las redes.
 * */
@Entity(
    tableName = "users_data_bytes",
    primaryKeys = ["sim_id", "type"],
    foreignKeys = [
        ForeignKey(
            entity = Sim::class,
            parentColumns = ["id"],
            childColumns = ["sim_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
@TypeConverters(UserDataBytes.DataTypeConverter::class)
data class UserDataBytes(
    @ColumnInfo(name = "sim_id")
    var simId: String,
    val type: DataType,
    @ColumnInfo(name = "initial_bytes")
    var initialBytes: Long,
    var bytes: Long,
    @ColumnInfo(name = "bytes_lte")
    var bytesLte: Long,
    @ColumnInfo(name = "start_time")
    var startTime: Long,
    @ColumnInfo(name = "expired_time")
    var expiredTime: Long
) {

    @Ignore
    lateinit var sim: Sim

    val priority: Int
        get() = when (type) {
            DataType.National -> 0
            DataType.DailyBag -> 1
            DataType.Bonus -> 2
            DataType.PromoBonus -> 3
            DataType.International -> 4
        }

    enum class DataType {
        International,
        Bonus,
        PromoBonus,
        National,
        DailyBag
    }

    fun exists() = bytes != 0L || bytesLte != 0L

    fun isExpired() = expiredTime != 0L && Date().after(Date(expiredTime))

    override fun equals(other: Any?): Boolean {
        if (other is UserDataBytes && other.type == type && other.simId == simId)
            return true
        return false
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + simId.hashCode()
        return result
    }

    class DataTypeConverter {

        @TypeConverter
        fun toType(value: String): DataType =
            DataType.valueOf(value)

        @TypeConverter
        fun fromType(type: DataType): String =
            type.name
    }
}