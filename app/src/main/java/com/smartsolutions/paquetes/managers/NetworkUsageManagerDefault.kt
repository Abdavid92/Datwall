package com.smartsolutions.paquetes.managers

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.M)
class NetworkUsageManagerDefault @Inject constructor(
    @ApplicationContext
    context: Context,
    simManager : ISimManager
): AbstractNetworkUsage(
    ContextCompat.getSystemService(context, NetworkStatsManager::class.java) ?: throw NullPointerException(),
    ContextCompat.getSystemService(context, TelephonyManager::class.java) ?: throw NullPointerException(),
    simManager
) {

    override suspend fun getAppUsage(uid : Int, start: Long, finish: Long, updateSim: Boolean): Traffic {
        if (updateSim){
            updateSimID()
        }
        val buckets = getUsage(start, finish)
        val traffic = Traffic(uid, 0L, 0L, simId ?: "")
        traffic.startTime = start
        traffic.endTime = finish
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
        updateSimID()
        val result = mutableListOf<Traffic>()

        getUsage(start, finish)?.let { buckets ->
            buckets.forEach { bucket ->
                var traffic = result.firstOrNull { it.uid == bucket.uid }

                if (traffic == null) {
                    traffic = Traffic(bucket.uid, bucket.rxBytes, bucket.txBytes, simId ?: "")
                    result.add(traffic)
                } else {
                    traffic += bucket
                }
            }
        }

        return result
    }

    override suspend fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long) {
        updateSimID()
        apps.forEach { iapp ->
            if (iapp is App)
                iapp.traffic = getAppUsage(iapp.uid, start, finish, false)
            else if (iapp is AppGroup)
                iapp.forEach { app ->
                    app.traffic = getAppUsage(app.uid, start, finish, false)
                }
        }
    }

    override suspend fun getUsageTotal(start : Long, finish : Long) : Traffic {
        updateSimID()
        getUsageGeneral(start, finish)?.let {
            val traffic = Traffic(0, it.rxBytes, it.txBytes, simId  ?: "")
            traffic.startTime = start
            traffic.endTime = finish
            return traffic
        }
        return Traffic(0, 0L, 0L, simId ?: "")
    }
}