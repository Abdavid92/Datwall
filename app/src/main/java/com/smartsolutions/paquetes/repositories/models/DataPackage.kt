package com.smartsolutions.paquetes.repositories.models

import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.StringDef
import androidx.lifecycle.LiveData
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import com.smartsolutions.paquetes.annotations.Networks
import kotlinx.parcelize.IgnoredOnParcel

/**
 * Entidad correspondiente a la tabla data_packages
 * */
@Entity(tableName = "data_packages")
data class DataPackage(
    /**
     * Id del paquete.
     * */
    @PrimaryKey
    val id: String,
    /**
     * Nombre.
     * */
    val name: String,
    /**
     * Descripción.
     * */
    val description: String,
    /**
     * Precio de compra.
     * */
    val price: Float,
    /**
     * Bytes disponibles en todas la redes.
     * */
    val bytes: Long,
    /**
     * Bytes disponibles en la red 4G
     * */
    @ColumnInfo(name = "bytes_lte")
    val bytesLte: Long,
    /**
     * Bono.
     * */
    @ColumnInfo(name = "bonus_bytes")
    @SerializedName("bonus_bytes")
    val bonusBytes: Long,
    /**
     * Bytes de navegación nacional.
     * */
    @ColumnInfo(name = "bonus_cu_bytes")
    @SerializedName("bonus_cu_bytes")
    val nationalBytes: Long,
    /**
     * Redes en la que está disponible este paquete.
     * */
    @Networks
    val network: String,
    /**
     * Índice en el menu de compra. Esto se usa para contruir el
     * código ussd.
     * */
    val index: Int,
    /**
     * Duración en dias.
     * */
    val duration: Int,
    /**
     * Indica si este paquete está obsoleto.
     * */
    var deprecated: Boolean = false
) : Parcelable {

    @Ignore
    var ussd: String? = null

    @Ignore
    var url: String? = null

    @Ignore
    var sims = emptyList<Sim>()

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: throw NullPointerException(),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readFloat(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    ) {
        sims = parcel.createTypedArrayList(Sim) ?: throw NullPointerException()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeFloat(price)
        parcel.writeLong(bytes)
        parcel.writeLong(bytesLte)
        parcel.writeLong(bonusBytes)
        parcel.writeLong(nationalBytes)
        parcel.writeString(network)
        parcel.writeInt(index)
        parcel.writeInt(duration)
        parcel.writeByte(if (deprecated) 1 else 0)
        parcel.writeTypedList(sims)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<DataPackage> {
        override fun createFromParcel(parcel: Parcel): DataPackage {
            return DataPackage(parcel)
        }

        override fun newArray(size: Int): Array<DataPackage?> {
            return arrayOfNulls(size)
        }
    }
}