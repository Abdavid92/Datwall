package com.smartsolutions.datwall.managers

import com.smartsolutions.datwall.managers.models.Traffic
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.IApp
import java.util.*

interface INetworkUsageManager {
    fun getAppUsage(uid : Int, start: Long, finish: Long): Traffic

    fun getAppsUsage(start: Long, finish: Long): List<Traffic>

    fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long)

    fun getUsageTotal(start : Long, finish : Long) : Traffic

    fun getAppPerConsumed(apps: List<App>, start: Long, finish: Long, moreConsumed : Boolean) : App?

    fun getAppUsageDayByHour(uid: Int, day : Date = Date()) : List<Pair<String, Traffic>>
}