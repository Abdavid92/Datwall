package com.smartsolutions.paquetes.watcher

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.internalDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

class WatcherUtils @Inject constructor(
    @ApplicationContext
    private val context: Context
) {

    private val usageStatsManager = ContextCompat
        .getSystemService(context, UsageStatsManager::class.java)

    private val activityManager by lazy {
        ContextCompat.getSystemService(context, ActivityManager::class.java)
            ?: throw NullPointerException()
    }

    private val packageManager = context.packageManager

    suspend fun getLastApp(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            getLollipopMr1LastApp()
        else
            getLollipopLastApp()
    }

    @Suppress("DEPRECATION")
    private fun getLollipopLastApp(): String? {
        val tasks = try {
            activityManager.getRecentTasks(1, ActivityManager.RECENT_IGNORE_UNAVAILABLE)
        } catch (e: SecurityException) {
            emptyList<ActivityManager.RecentTaskInfo>()
        }

        if (tasks.isNotEmpty()) {
            return tasks[0].baseIntent.component?.packageName
        }
        return null
    }

    private suspend fun getLollipopMr1LastApp(): String? {

        val isModern = context.internalDataStore.data.firstOrNull()?.get(PreferencesKeys.IS_FOREGROUND_APP_MODERN)
            ?: (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)

        return if (isModern){
            lastAppModeModern()
        }else {
            lastAppModeAncient()
        }
    }

    private fun lastAppModeAncient(): String? {
        if (usageStatsManager != null) {
            val time = System.currentTimeMillis()

            val statsList = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                time - 10000,
                time
            )

            if (statsList.isNotEmpty()) {
                var stats = statsList[0]
                if (statsList.size > 1) {
                    for (i in 1 until statsList.size) {
                        if (statsList[i].lastTimeUsed > stats.lastTimeUsed) {
                            stats = statsList[i]
                        }
                    }
                }
                return stats.packageName
            }
        }
        return null
    }


    private fun lastAppModeModern(): String? {
        if (usageStatsManager != null) {
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