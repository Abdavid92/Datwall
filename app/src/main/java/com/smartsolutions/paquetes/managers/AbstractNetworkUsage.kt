package com.smartsolutions.paquetes.managers

import android.Manifest
import android.annotation.SuppressLint
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.net.ConnectivityManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import dagger.Lazy
import org.apache.commons.lang.time.DateUtils
import java.util.*
import kotlin.reflect.KProperty

@RequiresApi(23)
@SuppressLint("HardwareIds")
abstract class AbstractNetworkUsage(
    private val networkStatsManager: NetworkStatsManager,
    telephonyManager: TelephonyManager,
    simManager: ISimManager
) : NetworkUsageManager(simManager) {

    private var subscriberId = object : Lazy<String?> {

        private var instance: String? = null

        override fun get(): String? {
            if (instance == null && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                instance = try {
                    telephonyManager.subscriberId
                } catch (e: SecurityException) {
                    throw MissingPermissionException(Manifest.permission.READ_PHONE_STATE)
                }
            }
            return instance
        }
    }

    /**
     * Cache que contiene los buckets por aplicación que se pidieron con antelación.
     * */
    private val cache = mutableListOf<BucketCache<List<NetworkStats.Bucket>>>()

    /**
     * Cache que contiene los buckets generales que se pidieron con antelación.
     * */
    private val generalCache = mutableListOf<BucketCache<NetworkStats.Bucket>>()

    /**
     * Retorna los buckets con el registro de las app que han consumido en el periodo de tiempo especificado
     * */
    protected fun getUsage(start: Long, finish: Long): List<NetworkStats.Bucket>? {
        clearCache()

        cache.firstOrNull{ it.simId ==  simId && it.start == start && it.finish == finish }?.let {
            return it.buckets
        }

        val buckets: MutableList<NetworkStats.Bucket> = ArrayList()
        try {

            val networkStats = networkStatsManager.querySummary(ConnectivityManager.TYPE_MOBILE, subscriberId.get(), start, finish)

            while (networkStats.hasNextBucket()) {

                val bucket = NetworkStats.Bucket()
                if (networkStats.getNextBucket(bucket)) {
                    buckets.add(bucket)
                }
            }
        } catch (e: Exception) {
            return null
        }

        cache.add(BucketCache(simId, System.currentTimeMillis(), start, finish, buckets))

        return buckets
    }

    //Retorna el bucket de consumo en general en el periodo especificado
    protected fun getUsageGeneral(start: Long, finish: Long): NetworkStats.Bucket? {
        clearCache()

        generalCache.firstOrNull{ it.simId == simId && it.start == start && it.finish == finish }?.let {
            return it.buckets
        }

        return try {
            val result = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, subscriberId.get(), start, finish)

            generalCache.add(BucketCache(simId, System.currentTimeMillis(), start, finish, result))

            result
        } catch (e: Exception) {
            null
        }
    }

    private fun clearCache() {
        val cacheToClear = mutableListOf<BucketCache<List<NetworkStats.Bucket>>>()
        val generalCacheToClear = mutableListOf<BucketCache<NetworkStats.Bucket>>()

        cache.forEach {
            if ((System.currentTimeMillis() - it.queryTime) > DateUtils.MILLIS_PER_DAY){
                cacheToClear.add(it)
            }
        }
        generalCache.forEach {
            if ((System.currentTimeMillis() - it.queryTime) > DateUtils.MILLIS_PER_DAY){
                generalCacheToClear.add(it)
            }
        }

        cache.removeAll(cacheToClear)
        generalCache.removeAll(generalCacheToClear)
    }

    internal data class BucketCache<T>(
        val simId: String,
        val queryTime: Long,
        val start: Long,
        val finish: Long,
        val buckets: T
    )
}
