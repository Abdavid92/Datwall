package com.smartsolutions.paquetes.repositories.models

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.SerializedName
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages

/**
 * Entidad correspondiente a la tabla data_packages
 * */
@Entity(tableName = "data_packages")
@TypeConverters(DataPackage.PackageIdConverter::class)
data class DataPackage(
    /**
     * Id del paquete.
     * */
    @PrimaryKey
    override val id: DataPackages.PackageId,
    /**
     * Nombre.
     * */
    override val name: String,
    /**
     * Descripción.
     * */
    override val description: String,
    /**
     * Precio de compra.
     * */
    override val price: Float,
    /**
     * Bytes disponibles en todas la redes.
     * */
    override val bytes: Long,
    /**
     * Bytes disponibles en la red 4G
     * */
    @ColumnInfo(name = "bytes_lte")
    override val bytesLte: Long,
    /**
     * Bytes de navegación nacional.
     * */
    @ColumnInfo(name = "bonus_cu_bytes")
    @SerializedName("bonus_cu_bytes")
    override val nationalBytes: Long,
    /**
     * Redes en la que está disponible este paquete.
     * */
    @Networks
    override val network: String,
    /**
     * Índice en el menu de compra. Esto se usa para contruir el
     * código ussd.
     * */
    override val index: Int,
    /**
     * Duración en dias.
     * */
    override val duration: Int,
    /**
     * Clave que identifica el paquete en el sms de compra.
     * */
    override val smsKey: String,
    /**
     * Indica si este paquete está obsoleto.
     * */
    override var deprecated: Boolean = false,
    /**
     * Minutos que incluye si es un plan combinado
     */
    override val minutes: Int = 0,
    /**
     * SMS que incluye si es un plan combinado
     */
    override val sms: Int = 0
) : IDataPackage, Parcelable {

    @Ignore
    var ussd: String? = null

    @Ignore
    var url: String? = null

    @Ignore
    var sims = emptyList<Sim>()

    constructor(parcel: Parcel) : this(
        PackageIdConverter().toPackageId(parcel.readInt()),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readFloat(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readInt(),
        parcel.readInt(),
        parcel.readString() ?: throw NullPointerException(),
        parcel.readByte() != 0.toByte(),
        parcel.readInt(),
        parcel.readInt()
    ) {
        sims = parcel.createTypedArrayList(Sim) ?: throw NullPointerException()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(PackageIdConverter().fromPackageId(id))
        parcel.writeString(name)
        parcel.writeString(description)
        parcel.writeFloat(price)
        parcel.writeLong(bytes)
        parcel.writeLong(bytesLte)
        parcel.writeLong(nationalBytes)
        parcel.writeString(network)
        parcel.writeInt(index)
        parcel.writeInt(duration)
        parcel.writeString(smsKey)
        parcel.writeByte(if (deprecated) 1 else 0)
        parcel.writeTypedList(sims)
        parcel.writeInt(minutes)
        parcel.writeInt(sms)
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

    class PackageIdConverter {

        @TypeConverter
        fun toPackageId(value: Int): DataPackages.PackageId {
            return DataPackages.PackageId.values()[value]
        }

        @TypeConverter
        fun fromPackageId(packageId: DataPackages.PackageId): Int {
            return packageId.ordinal
        }
    }
}