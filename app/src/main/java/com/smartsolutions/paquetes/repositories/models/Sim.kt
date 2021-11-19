package com.smartsolutions.paquetes.repositories.models

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.smartsolutions.paquetes.annotations.Networks

/**
 * Tabla de las lineas instaladas en el dispositivo.
 * */
@Entity(tableName = "sims")
data class Sim(
    /**
     * Id de la linea. Puede ser el cardId(android 30 o superior) o
     * el iccId(android 29 o menor) del subscritionInfo
     * utilizado para obtener la linea.
     * */
    @PrimaryKey
    val id: String,
    /**
     * Fecha en la que se configuró la linea para obtener los tipos de
     * redes disponibles para ella. Si su valor es cero significa
     * que no se ha configurado.
     * */
    @ColumnInfo(name = "setup_date")
    var setupDate: Long,
    /**
     * Tipos de redes disponibles para la linea.
     * Cuando no se han configurado los paquetes para esta línea el valor de
     * esta propieda será [Networks.NETWORK_NONE].
     * */
    @Networks
    var network: String
) : Parcelable {

    /**
     * Número de teléfono Si es null significa que no está disponible.
     * */
    var phone: String? = null

    /**
     * Última sincronización de los [UserDataBytes] de esta linea.
     * */
    @ColumnInfo(name = "last_synchronization")
    var lastSynchronization: Long = 0

    /**
     * Ícono de la linea.
     * */
    @Ignore
    var icon: Bitmap? = null

    /**
     * Paquetes disponibles para esta linea.
     * */
    @Ignore
    var packages = emptyList<DataPackage>()

    @Ignore
    var slotIndex: Int = 1

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: throw NullPointerException(),
        parcel.readLong(),
        parcel.readString() ?: throw NullPointerException()
    ) {
        phone = parcel.readString()
        icon = parcel.readParcelable(Bitmap::class.java.classLoader)
        packages = parcel.createTypedArrayList(DataPackage.CREATOR) ?: throw NullPointerException()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeLong(setupDate)
        parcel.writeString(network)
        parcel.writeString(phone)
        parcel.writeParcelable(icon, flags)
        parcel.writeTypedList(packages)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Sim

        if (id != other.id) return false
        if (setupDate != other.setupDate) return false
        if (network != other.network) return false
        if (phone != other.phone) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + setupDate.hashCode()
        result = 31 * result + network.hashCode()
        result = 31 * result + (phone?.hashCode() ?: 0)
        return result
    }

    companion object CREATOR : Parcelable.Creator<Sim> {
        override fun createFromParcel(parcel: Parcel): Sim {
            return Sim(parcel)
        }

        override fun newArray(size: Int): Array<Sim?> {
            return arrayOfNulls(size)
        }
    }
}