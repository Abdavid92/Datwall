package com.smartsolutions.paquetes.managers

import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.RemoteException
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.helpers.SimDelegate
import org.apache.commons.lang.time.DateUtils
import java.util.ArrayList

@RequiresApi(23)
@SuppressLint("HardwareIds")
abstract class NetworkUsageDigger(
    private val networkStatsManager: NetworkStatsManager,
    telephonyManager: TelephonyManager,
    private val simDelegate: SimDelegate
) : NetworkUsageManager() {
    private var ID: String? = null

    private var cacheSim1 = mutableListOf<BucketCache<List<NetworkStats.Bucket>>>()
    private var generalCacheSim1 = mutableListOf<BucketCache<NetworkStats.Bucket>>()
    private var cacheSim2 = mutableListOf<BucketCache<List<NetworkStats.Bucket>>>()
    private var generalCacheSim2 = mutableListOf<BucketCache<NetworkStats.Bucket>>()

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

        val simIndex = simDelegate.getActiveDataSimIndex()

        if (simIndex == 1) {
            cacheSim1.firstOrNull { it.start == start && it.finish == finish }?.let {
                return it.buckets
            }
        }else {
            cacheSim2.firstOrNull { it.start == start && it.finish == finish }?.let {
                return it.buckets
            }
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

        if (simIndex == 1) {
            cacheSim1.add(BucketCache(System.currentTimeMillis(), start, finish, buckets))
        }else {
            cacheSim2.add(BucketCache(System.currentTimeMillis(), start, finish, buckets))
        }

        return buckets
    }

    //Retorna el bucket de consumo en general en el periodo especificado
    fun getUsageGeneral(start: Long, finish: Long): NetworkStats.Bucket? {
        clearGeneralCache()

        val simIndex = simDelegate.getActiveDataSimIndex()

        if (simIndex == 1) {
            generalCacheSim1.firstOrNull { it.start == start && it.finish == finish }?.let {
                return it.buckets
            }
        }else {
            generalCacheSim2.firstOrNull { it.start == start && it.finish == finish }?.let {
                return it.buckets
            }
        }

        return try {
            val result = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, ID, start, finish)
            if (simIndex == 1) {
                generalCacheSim1.add(BucketCache(System.currentTimeMillis(), start, finish, result))
            }else {
                generalCacheSim2.add(BucketCache(System.currentTimeMillis(), start, finish, result))
            }
            result
        } catch (e: RemoteException) {
            null
        }
    }

    private fun clearCache() {

        val clearListSim1 = mutableListOf<BucketCache<*>>()
        val clearListSim2 = mutableListOf<BucketCache<*>>()

        cacheSim1.forEach {
            if (System.currentTimeMillis() - it.queryTime >= DateUtils.MILLIS_PER_DAY) {
                clearListSim1.add(it)
            }
        }

        cacheSim2.forEach {
            if (System.currentTimeMillis() - it.queryTime >= DateUtils.MILLIS_PER_DAY) {
                clearListSim2.add(it)
            }
        }

        clearListSim1.forEach {
            cacheSim1.remove(it)
        }

        clearListSim2.forEach {
            cacheSim2.remove(it)
        }
    }

    private fun clearGeneralCache() {

        val clearListSim1 = mutableListOf<BucketCache<*>>()
        val clearListSim2 = mutableListOf<BucketCache<*>>()

        generalCacheSim1.forEach {
            if (System.currentTimeMillis() - it.queryTime >= DateUtils.MILLIS_PER_DAY) {
                clearListSim1.add(it)
            }
        }

        generalCacheSim2.forEach {
            if (System.currentTimeMillis() - it.queryTime >= DateUtils.MILLIS_PER_DAY) {
                clearListSim2.add(it)
            }
        }

        clearListSim1.forEach {
            generalCacheSim1.remove(it)
        }

        clearListSim2.forEach {
            generalCacheSim2.remove(it)
        }
    }

    internal data class BucketCache<T>(
        val queryTime: Long,
        val start: Long,
        val finish: Long,
        val buckets: T
    )
}