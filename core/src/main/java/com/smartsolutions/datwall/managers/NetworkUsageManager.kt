package com.smartsolutions.datwall.managers

import com.smartsolutions.datwall.managers.models.Traffic
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.IApp
import org.apache.commons.lang.time.DateUtils
import java.util.*

abstract class NetworkUsageManager {
    abstract suspend fun getAppUsage(uid : Int, start: Long, finish: Long): Traffic

    abstract suspend fun getAppsUsage(start: Long, finish: Long): List<Traffic>

    abstract suspend fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long)

    abstract suspend fun getUsageTotal(start : Long, finish : Long) : Traffic

    suspend fun getAppPerConsumed(apps: List<App>, start: Long, finish: Long, moreConsumed : Boolean) : App?{
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

    suspend fun getAppUsageDayByHour(uid: Int, day : Date) : List<Pair<Long, Traffic>>{
        val pairList: ArrayList<Pair<Long, Traffic>> = ArrayList()
        var date = NetworkUtils.getZeroHour(day)

        while (DateUtils.isSameDay(date, day) && date.time <= System.currentTimeMillis()){
            val start = date.time
            date = DateUtils.setMinutes(date, 59)
            val finish = date.time
            pairList.add(Pair(date.time, getAppUsage(uid, start, finish)))
            date = DateUtils.addHours(date, 1)
            date = DateUtils.setMinutes(date, 0)
        }

        return pairList
    }
}