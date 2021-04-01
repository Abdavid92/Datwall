package com.smartsolutions.datwall.managers

import android.app.usage.NetworkStatsManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.smartsolutions.datwall.managers.models.Traffic
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.M)
class NetworkUsageManager @Inject constructor(
    networkStatsManager: NetworkStatsManager,
    telephonyManager: TelephonyManager
): NetworkUsageDigger(networkStatsManager, telephonyManager) {

    fun getAppUsage(uid : Int, start: Long, finish: Long): Traffic {
        val traffic = Traffic(uid, 0L, 0L)
        traffic.startTime = start
        traffic.endTime = finish
        val buckets = getUsage(start, finish)
        buckets?.let {bucketsList ->
            bucketsList.forEach { bucket ->
                if (bucket.uid == uid) {
                    traffic += bucket
                }
            }
        }
        return traffic
    }

    fun getAppsUsage(start: Long, finish: Long): List<Traffic> {
        val result = mutableListOf<Traffic>()

        val buckets = getUsage(start, finish)

        buckets?.let { bucketList ->

            bucketList.forEach { bucket ->
                var traffic = result.firstOrNull { it.uid == bucket.uid }

                if (traffic == null) {
                    traffic = Traffic(bucket.uid, bucket.rxBytes, bucket.txBytes)
                    result.add(traffic)
                } else {
                    traffic += bucket
                }
            }

        }

        return result
    }


}