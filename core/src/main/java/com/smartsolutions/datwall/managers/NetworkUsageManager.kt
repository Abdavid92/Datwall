package com.smartsolutions.datwall.managers

import android.app.usage.NetworkStatsManager
import android.os.Build
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import com.smartsolutions.datwall.managers.models.Traffic
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList

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

    fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long) {
        apps.forEach { iapp ->
            if (iapp is App)
                iapp.traffic = Traffic(iapp.uid, 0L, 0L)
            else if (iapp is AppGroup)
                iapp.forEach { app ->
                    app.traffic = Traffic(app.uid, 0L, 0L)
                }
        }

        getUsage(start, finish)?.let { buckets ->
            buckets.forEach {bucket ->
                apps.firstOrNull { it.uid == bucket.uid }?.let { iapp ->
                    if (iapp is App)
                        iapp.traffic!! += bucket
                    else if (iapp is AppGroup)
                        iapp.forEach { app ->
                            app.traffic!! += bucket
                        }
                }
            }
        }
    }

    fun getUsageTotal(start : Long, finish : Long) : Traffic {
        getUsageGeneral(start, finish)?.let {
            val traffic = Traffic(0, it.rxBytes, it.txBytes)
            traffic.startTime = start
            traffic.endTime = finish
            return traffic
        }
        return Traffic(0, 0L, 0L)
    }

    fun getAppPerConsumed(apps: List<App>, start: Long, finish: Long, moreConsumed : Boolean) : App?{
        if (apps.isEmpty()){
            return null
        }
        fillAppsUsage(apps, start, finish)
        var app = apps[0]
        for (i in 1 .. apps.size ){
            if (moreConsumed) {
                if (apps[i].traffic!! > app.traffic!!) {
                    app = apps[i]
                }
            }else {
                if (apps[i].traffic!! < app.traffic!!) {
                    app = apps[i]
                }
            }
        }
        return app
    }

    fun getAppUsageDayByHour(uid: Int, day : Date = Date()) : List<Pair<String, Traffic>>{
        val pairList: ArrayList<Pair<String, Traffic>> = ArrayList()
        var date = NetworkUtils.getZeroHour(day)

        while (DateUtils.isSameDay(date, day) && date.time <= System.currentTimeMillis()){
            val start = date.time
            date = DateUtils.setMinutes(date, 59)
            val finish = date.time
            pairList.add(Pair(SimpleDateFormat("hh aa", Locale.US).format(date), getAppUsage(uid, start, finish)))
            date = DateUtils.addHours(date, 1)
            date = DateUtils.setMinutes(date, 0)
        }

        return pairList
    }
}