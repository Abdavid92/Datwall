package com.smartsolutions.datwall.watcher

import android.app.ActivityManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import javax.inject.Inject

class WatcherUtils @Inject constructor(
    private val usageStatsManager: UsageStatsManager,
    private val activityManager: ActivityManager,
    private val packageManager: PackageManager
) {
    
    fun getLastApp(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

            val time = System.currentTimeMillis()

            val statsList = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 10000, time)

            if (statsList.isNotEmpty()) {
                var stats = statsList[0]

                if (statsList.size > 1) {

                    statsList.forEach {
                        if (it.lastTimeUsed > stats.lastTimeUsed)
                            stats = it
                    }
                }
                return stats.packageName
            }

        } else {
            val tasks = activityManager.getRecentTasks(1, ActivityManager.RECENT_IGNORE_UNAVAILABLE)

            if (tasks.isNotEmpty()) {
                return tasks[0].baseIntent.component?.packageName
            }
        }
        return null
    }

    fun isTheLauncher(packageName: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)

        val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)

        return resolveInfo != null && resolveInfo.activityInfo.packageName == packageName
    }
}