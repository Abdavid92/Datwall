package com.smartsolutions.paquetes.managers.models

import android.app.usage.NetworkStats
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartsolutions.paquetes.helpers.DataUnit
import com.smartsolutions.paquetes.helpers.DataValue
import com.smartsolutions.paquetes.helpers.processValue
import org.apache.commons.lang.time.DateUtils
import java.util.*

/**
 * Representa un fragmento de tráfico de datos tanto
 * para una aplicación como para todas la aplicaciones,
 * dependiendo si uid es distinto de cero.
 * */
@Entity(tableName = "traffic")
open class Traffic(
    /**
     * Uid de la aplicación que pertenece el tráfico de datos.
     * Cero si no pertenece a ninguna.*/
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
    var _txBytes : Long
    ) : Parcelable {

    /**
     * Id auto-generado proveniente de la base de datos
     * */
    @PrimaryKey(autoGenerate = true)
    var id : Long = 0L

    /**
     * Tiempo de inicio en que se transmitió este fragmento de tráfico
     * */
    @ColumnInfo(name = "start_time")
    var startTime : Long = 0L

    /**
     * Tiempo en que se cerró este fragmento de tráfico
     * */
    @ColumnInfo(name = "end_time")
    var endTime : Long = 0L

    /**
     * Bytes de bajada optimizados a la unidad más conveniente
     * */
    val rxBytes : DataValue
        get() = processValue(_rxBytes)

    /**
     * Bytes de subida optimizados a la unidad más conveniente
     * */
    val txBytes : DataValue
        get() = processValue(_txBytes)

    /**
     * Suma de todos los bytes optimizados a la unidad más conveniente
     * */
    val totalBytes : DataValue
        get() = processValue(_rxBytes + _txBytes)

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong()) {
        startTime = parcel.readLong()
        endTime = parcel.readLong()
    }

    constructor() : this (
        0, 0L, 0L
            )


    /**
     * @return Bytes de bajada optimizados a la unidad dada como parámetro.
     *
     * @param unit - Unidad de medida
     * */
    fun rxBytes (unit: DataUnit) = processValue(_rxBytes, unit)

    /**
     * @return Bytes de subida optimizados a la unidad dada como parámetro.
     *
     * @param unit - Unidad de medida
     * */
    fun txBytes (unit: DataUnit) = processValue(_txBytes, unit)

    /**
     * @return Suma de todos los bytes optimizados a la unidad dada como parámetro.
     *
     * @param unit - Unidad de medida
     * */
    fun totalBytes (unit: DataUnit) = processValue(_txBytes + _rxBytes, unit)

    /**
     * @return Suma de bytes
     * */
    fun getAllBytes () : Long {
        return _rxBytes + _txBytes
    }

    @RequiresApi(Build.VERSION_CODES.M)
    open operator fun plusAssign(bucket: NetworkStats.Bucket){
        if (isInDiscountHour(bucket)){
            this._rxBytes += bucket.rxBytes / 2
            this._txBytes += bucket.txBytes / 2
        }else {
            this._rxBytes += bucket.rxBytes
            this._txBytes += bucket.txBytes
        }
    }


    operator fun plusAssign(traffic: Traffic){
        this._rxBytes += traffic._rxBytes
        this._txBytes += traffic._txBytes
    }


    operator fun compareTo (traffic: Traffic) : Int{
        val selfTotal = this._rxBytes + this._txBytes
        val otherTotal = traffic._rxBytes + traffic._txBytes

        return when {
            selfTotal > otherTotal -> 1
            selfTotal < otherTotal -> -1
            else -> 0
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isInDiscountHour (bucket: NetworkStats.Bucket) : Boolean{
        var startTime = DateUtils.setHours(Date(bucket.startTimeStamp), 1)
        startTime = DateUtils.setMinutes(startTime, 0)
        startTime = DateUtils.setSeconds(startTime, 1)

        var finishTime = DateUtils.setHours(Date(bucket.startTimeStamp), 6)
        finishTime = DateUtils.setMinutes(finishTime, 0)
        finishTime = DateUtils.setSeconds(finishTime, 1)

        return Date(bucket.startTimeStamp).after(startTime) && Date(bucket.endTimeStamp).before(finishTime)
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(uid)
        parcel.writeLong(_rxBytes)
        parcel.writeLong(_txBytes)
        parcel.writeLong(startTime)
        parcel.writeLong(endTime)
    }

    override fun describeContents(): Int {
        return 0
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