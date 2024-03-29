package com.smartsolutions.paquetes.managers.models

import android.app.usage.NetworkStats
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.room.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.repositories.models.Sim

/**
 * Representa un fragmento de tráfico de datos tanto
 * para una aplicación como para todas la aplicaciones,
 * dependiendo si uid es distinto de cero.
 * */
@Entity(
    tableName = "traffic", 
    foreignKeys = [
        ForeignKey(
            entity = Sim::class, 
            parentColumns = ["id"], 
            childColumns = ["sim_id"])
    ],
    indices = [
        Index("sim_id", unique = false)
    ]
)
open class Traffic(
    /**
     * Uid de la aplicación que pertenece el tráfico de datos.
     * Cero si no pertenece a ninguna.
     * */
    val uid: Int,
    /**
     * Tráfico de descarga en bytes
     * */
    @ColumnInfo(name = "rx_bytes")
    var _rxBytes : Long,
    /**
     * Tráfico de subida en bytes
     * */
    @ColumnInfo(name = "tx_bytes")
    var _txBytes : Long,
    /**
     * Id de la sim en la que se realizó el tráfico
     */
    @ColumnInfo(name = "sim_id")
    val simId: String
    ) : Parcelable {

    /**
     * Id auto-generado proveniente de la base de datos
     * */
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0L

    /**
     * Tiempo de inicio en que se transmitió este fragmento de tráfico
     * */
    @ColumnInfo(name = "start_time")
    var startTime: Long = 0L

    /**
     * Tiempo en que se cerró este fragmento de tráfico
     * */
    @ColumnInfo(name = "end_time")
    var endTime: Long = 0L

    @Networks
    var network: String = Networks.NETWORK_NONE



    /**
     * Bytes de bajada optimizados a la unidad más conveniente
     * */
    val rxBytes: DataUnitBytes
        get() = DataUnitBytes(_rxBytes)

    /**
     * Bytes de subida optimizados a la unidad más conveniente
     * */
    val txBytes: DataUnitBytes
        get() = DataUnitBytes(_txBytes)

    /**
     * Suma de todos los bytes optimizados a la unidad más conveniente
     * */
    val totalBytes: DataUnitBytes
        get() = DataUnitBytes(_rxBytes + _txBytes)

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong(),
        parcel.readString() ?: ""
    ) {
        id = parcel.readLong()
        startTime = parcel.readLong()
        endTime = parcel.readLong()
        network = parcel.readString() ?: Networks.NETWORK_NONE
    }

    constructor() : this(
        0, 0L, 0L, ""
    )

    @RequiresApi(Build.VERSION_CODES.M)
    open operator fun plusAssign(bucket: NetworkStats.Bucket) {
        if (isInDiscountHour(bucket)) {
            this._rxBytes += bucket.rxBytes / 2
            this._txBytes += bucket.txBytes / 2
        } else {
            this._rxBytes += bucket.rxBytes
            this._txBytes += bucket.txBytes
        }
    }


    operator fun plusAssign(traffic: Traffic) {
        this._rxBytes += traffic._rxBytes
        this._txBytes += traffic._txBytes
    }


    operator fun compareTo(traffic: Traffic): Int {
        val selfTotal = this._rxBytes + this._txBytes
        val otherTotal = traffic._rxBytes + traffic._txBytes

        return when {
            selfTotal > otherTotal -> 1
            selfTotal < otherTotal -> -1
            else -> 0
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isInDiscountHour(bucket: NetworkStats.Bucket): Boolean =
        DateCalendarUtils.isInDiscountHour(bucket.startTimeStamp, bucket.endTimeStamp)

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(uid)
        parcel.writeLong(_rxBytes)
        parcel.writeLong(_txBytes)
        parcel.writeString(simId)
        parcel.writeLong(id)
        parcel.writeLong(startTime)
        parcel.writeLong(endTime)
        parcel.writeString(network)
    }

    override fun describeContents(): Int {
        return 0
    }

    operator fun minus(traffic: Traffic): Traffic {
        val trafficNew = Traffic(
            uid,
            _rxBytes - traffic._rxBytes,
            _txBytes - traffic._txBytes,
            simId
        )

        trafficNew.startTime = startTime
        trafficNew.endTime = endTime
        trafficNew.network = network

        return trafficNew
    }

    companion object CREATOR : Parcelable.Creator<Traffic> {
        override fun createFromParcel(parcel: Parcel): Traffic {
            return Traffic(parcel)
        }

        override fun newArray(size: Int): Array<Traffic?> {
            return arrayOfNulls(size)
        }
    }
}