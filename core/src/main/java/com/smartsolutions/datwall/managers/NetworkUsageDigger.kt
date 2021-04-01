package com.smartsolutions.datwall.managers

import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import org.apache.commons.lang.time.DateUtils
import java.util.ArrayList

@RequiresApi(23)
@SuppressLint("HardwareIds")
abstract class NetworkUsageDigger(
    private val networkStatsManager: NetworkStatsManager,
    telephonyManager: TelephonyManager
) {
    private var ID: String? = null

    private var cache = mutableListOf<BucketCache<List<NetworkStats.Bucket>>>()
    private var generalCache = mutableListOf<BucketCache<NetworkStats.Bucket>>()

    init {
        if (Build.VERSION.SDK_INT < 29) {
            ID = telephonyManager.subscriberId
        }
    }

    /**
     * Retorna los buckets con el registro de las app que han consumido en el periodo de tiempo especificado
     * */
    fun getUsage(start: Long, finish: Long): List<NetworkStats.Bucket>? {

        clearCache()

        cache.firstOrNull { it.start == start && it.finish == finish }?.let {
            return it.buckets
        }

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

        cache.add(BucketCache(System.currentTimeMillis(), start, finish, buckets))

        return buckets
    }

    //Retorna el bucket de consumo en general en el periodo especificado
    fun getUsageGeneral(start: Long, finish: Long): NetworkStats.Bucket? {
        clearGeneralCache()

        generalCache.firstOrNull { it.start == start && it.finish == finish }?.let {
            return it.buckets
        }

        return try {
            val result = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, ID, start, finish)
            generalCache.add(BucketCache(System.currentTimeMillis(), start, finish, result))
            result
        } catch (e: RemoteException) {
            null
        }
    }

    private fun clearCache() {

        val clearList = mutableListOf<BucketCache<*>>()

        cache.forEach {
            if (System.currentTimeMillis() - it.queryTime >= DateUtils.MILLIS_PER_DAY) {
                clearList.add(it)
            }
        }

        clearList.forEach {
            cache.remove(it)
        }
    }

    private fun clearGeneralCache() {

        val clearList = mutableListOf<BucketCache<*>>()

        generalCache.forEach {
            if (System.currentTimeMillis() - it.queryTime >= DateUtils.MILLIS_PER_DAY) {
                clearList.add(it)
            }
        }

        clearList.forEach {
            generalCache.remove(it)
        }
    }

    internal data class BucketCache<T>(
        val queryTime: Long,
        val start: Long,
        val finish: Long,
        val buckets: T
    )
}