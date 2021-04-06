package com.smartsolutions.datwall.managers

import android.content.Context
import com.smartsolutions.datwall.managers.models.Traffic
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.IApp
import java.util.*
import javax.inject.Inject

class LegacyNetworkUsageManager @Inject constructor(
    context: Context
): INetworkUsageManager {
    override fun getAppUsage(uid: Int, start: Long, finish: Long): Traffic {
        throw RuntimeException("Not supported")
    }

    override fun getAppsUsage(start: Long, finish: Long): List<Traffic> {
        TODO("Not yet implemented")
    }

    override fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long) {
        TODO("Not yet implemented")
    }

    override fun getUsageTotal(start: Long, finish: Long): Traffic {
        TODO("Not yet implemented")
    }

    override fun getAppPerConsumed(
        apps: List<App>,
        start: Long,
        finish: Long,
        moreConsumed: Boolean
    ): App? {
        TODO("Not yet implemented")
    }

    override fun getAppUsageDayByHour(uid: Int, day: Date): List<Pair<String, Traffic>> {
        TODO("Not yet implemented")
    }
}