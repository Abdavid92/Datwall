package com.smartsolutions.datwall.managers.models

import android.app.usage.NetworkStats
import android.os.Build
import androidx.annotation.RequiresApi

class InternationalTraffic(
    uid: Int,
    _rxBytes : Long,
    _txBytes : Long
) : Traffic(uid, _rxBytes, _txBytes) {

    @RequiresApi(Build.VERSION_CODES.M)
    override operator fun plusAssign(bucket: NetworkStats.Bucket){
        this._rxBytes += bucket.rxBytes
        this._txBytes += bucket.txBytes
    }
}