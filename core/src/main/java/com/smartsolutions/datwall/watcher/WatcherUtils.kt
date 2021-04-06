package com.smartsolutions.datwall.watcher

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import javax.inject.Inject

class WatcherUtils @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
    private val packageManager: PackageManager
) {

    fun getLastApp(): String? {
        val time = System.currentTimeMillis()

        val usageEvents = usageStatsManager.queryEvents(time - 1100, time)

        var event = UsageEvents.Event()

        if (usageEvents.hasNextEvent())
            usageEvents.getNextEvent(event)

        while (usageEvents.hasNextEvent()) {
            val newEvent = UsageEvents.Event()

            usageEvents.getNextEvent(newEvent)

            if (newEvent.eventType == UsageEvents.Event.ACTIVITY_RESUMED && newEvent.timeStamp > event.timeStamp)
                event = newEvent
        }

        if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED)
            return event.packageName

        return null
    }

    fun isTheLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)

        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfo != null && resolveInfo.activityInfo.packageName == packageName
    }
}