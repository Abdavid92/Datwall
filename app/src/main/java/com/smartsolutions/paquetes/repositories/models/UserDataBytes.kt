package com.smartsolutions.paquetes.repositories.models

import android.os.Parcel
import android.os.Parcelable
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
class UserDataBytes(
    /**
     * Id de la linea a la que pertenecen estos bytes.
     * */
    @ColumnInfo(name = "sim_id")
    var simId: String,
    /**
     * Tipo de bytes.
     * */
    type: DataType,
    /**
     * Bytes iniciales que estaban disponibles cuando se configuró o
     * se compro un paquete nuevo. Este campo se puede usar para comparar
     * los bytes iniciales con los actuales y saber cuánto se ha consumido.
     * */
    @ColumnInfo(name = "initial_bytes")
    var initialBytes: Long,
    /**
     * Bytes disponibles en todas las redes.
     * */
    bytes: Long,
    /**
     * Fecha en que obtuvieron los bytes.
     * */
    @ColumnInfo(name = "start_time")
    var startTime: Long,
    /**
     * Fechan en la que expiran los bytes y se pierden.
     * */
    expiredTime: Long
) : DataBytes(type, bytes, expiredTime), Parcelable {

    /**
     * Linea a la que pertenecen los bytes.
     * */
    @Ignore
    lateinit var sim: Sim

    /**
     * Prioridad de consumo.
     * */
    val priority: Int
        get() = when (type) {
            DataType.National -> 0 //Prioridad nula
            DataType.DailyBag -> 1
            DataType.InternationalLte -> 2
            DataType.PromoBonus -> 3
            DataType.International -> 4
        }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: throw NullPointerException(),
        DataType.valueOf(parcel.readString() ?: throw NullPointerException()),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong()
    ) {
        sim = parcel.readParcelable(Sim::class.java.classLoader) ?: throw NullPointerException()
    }

    /**
     * Indica si existen bytes a consumir.
     * */
    fun exists() = bytes != 0L

    /**
     * Indica si estos bytes están expirados.
     * */
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(simId)
        parcel.writeString(type.name)
        parcel.writeLong(initialBytes)
        parcel.writeLong(bytes)
        parcel.writeLong(startTime)
        parcel.writeLong(expiredTime)
        parcel.writeParcelable(sim, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UserDataBytes> {
        override fun createFromParcel(parcel: Parcel): UserDataBytes {
            return UserDataBytes(parcel)
        }

        override fun newArray(size: Int): Array<UserDataBytes?> {
            return arrayOfNulls(size)
        }
    }
}