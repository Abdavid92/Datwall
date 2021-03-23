package com.smartsolutions.paquetes.managers.models

import android.app.usage.NetworkStats
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.RequiresApi
import org.apache.commons.lang.time.DateUtils
import java.util.*
import kotlin.math.pow

class Traffic(val uid: Int, var _rxBytes : Long, var _txBytes : Long) : Parcelable {

    var startTime : Long = 0L

    var endTime : Long = 0L

    val rxBytes : Unity
        get() = proccesValue(_rxBytes)

    val txBytes : Unity
        get() = proccesValue(_txBytes)

    val totalBytes : Unity
        get() = proccesValue(_rxBytes + _txBytes)

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readLong(),
        parcel.readLong()) {
        startTime = parcel.readLong()
        endTime = parcel.readLong()
    }


    fun rxBytes (unit: Unit) = proccesValue(_rxBytes, unit)

    fun txBytes (unit: Unit) = proccesValue(_txBytes, unit)

    fun totalBytes (unit: Unit) = proccesValue(_txBytes + _rxBytes, unit)

    fun getAllBytes () : Long {
        return _rxBytes + _txBytes
    }


    @Suppress("NAME_SHADOWING")
    private fun proccesValue(bytes: Long, unit: Unit? = null) : Unity {
        val GB = 1024.0.pow(3.0)
        val MB = 1024.0.pow(2.0)

        var unit = unit

        if (unit == null){
            unit = when {
                GB <= bytes -> {
                    Unit.GB
                }
                MB <= bytes -> {
                    Unit.MB
                }
                else -> {
                    Unit.KB
                }
            }
        }

        val value = when (unit) {
            Unit.GB -> {
                bytes/GB
            }
            Unit.MB -> {
                bytes/MB
            }
            else -> {
                bytes/1024.0
            }
        }

        return Unity(value, unit)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    operator fun plusAssign(bucket: NetworkStats.Bucket){
        if (isInDiscountHour(bucket)){
            this._rxBytes += bucket.rxBytes/2
            this._txBytes += bucket.txBytes/2
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

    data class Unity(val value : Double, val unit: Unit)

    enum class Unit {
        KB, MB, GB
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