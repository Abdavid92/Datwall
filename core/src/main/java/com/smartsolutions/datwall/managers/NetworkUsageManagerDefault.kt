package com.smartsolutions.datwall.managers

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.datwall.managers.models.Traffic
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.lang.time.DateUtils
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

@RequiresApi(Build.VERSION_CODES.M)
class NetworkUsageManagerDefault @Inject constructor(
    @ApplicationContext
    context: Context
): NetworkUsageDigger(
    ContextCompat.getSystemService(context, NetworkStatsManager::class.java) ?: throw NullPointerException(),
    ContextCompat.getSystemService(context, TelephonyManager::class.java) ?: throw NullPointerException()
) {

    override suspend fun getAppUsage(uid : Int, start: Long, finish: Long): Traffic {
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

    override suspend fun getAppsUsage(start: Long, finish: Long): List<Traffic> {
        val result = mutableListOf<Traffic>()

        getUsage(start, finish)?.let { buckets ->
            buckets.forEach { bucket ->
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

    override suspend fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long) {
        apps.forEach { iapp ->
            if (iapp is App)
                iapp.traffic = getAppUsage(iapp.uid, start, finish)
            else if (iapp is AppGroup)
                iapp.forEach { app ->
                    app.traffic = getAppUsage(app.uid, start, finish)
                }
        }
    }

    override suspend fun getUsageTotal(start : Long, finish : Long) : Traffic {
        getUsageGeneral(start, finish)?.let {
            val traffic = Traffic(0, it.rxBytes, it.txBytes)
            traffic.startTime = start
            traffic.endTime = finish
            return traffic
        }
        return Traffic(0, 0L, 0L)
    }
}