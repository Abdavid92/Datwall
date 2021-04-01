package com.smartsolutions.datwall.managers

import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import java.util.ArrayList

@RequiresApi(23)
@SuppressLint("HardwareIds")
abstract class NetworkUsageDigger(
    private val networkStatsManager: NetworkStatsManager,
    telephonyManager: TelephonyManager
) {
    private var ID: String? = null

    init {
        if (Build.VERSION.SDK_INT < 29) {
            ID = telephonyManager.subscriberId
        }
    }

    /**
     * Retorna los buckets con el registro de las app que han consumido en el periodo de tiempo especificado
     * */
    fun getUsage(start: Long, finish: Long): List<NetworkStats.Bucket>? {
        val buckets: MutableList<NetworkStats.Bucket> = ArrayList()
        try {
            val networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, ID, start, finish)
            do {
                val bucket = NetworkStats.Bucket()
                if (networkStats.getNextBucket(bucket)) {
                    buckets.add(bucket)
                }
            } while (networkStats.hasNextBucket())
        } catch (e: RemoteException) {
            return null
        }
        return buckets
    }

    //Retorna el bucket de consumo en general en el periodo especificado
    fun getUsageGeneral(start: Long, finish: Long): NetworkStats.Bucket? {
        return try {
            networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, ID, start, finish)
        } catch (e: RemoteException) {
            null
        }
    }
}