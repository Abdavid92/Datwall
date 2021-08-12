package com.smartsolutions.paquetes.watcher

import android.app.ActivityManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.internal.DoubleCheck
import javax.inject.Inject
import javax.inject.Provider

class WatcherUtils @Inject constructor(
    @ApplicationContext
    context: Context
) {

    private val usageStatsManager = ContextCompat
        .getSystemService(context, UsageStatsManager::class.java)

    private val activityManager = object : Lazy<ActivityManager> {

        private var instance: ActivityManager? = null

        override fun get(): ActivityManager {
            if (instance == null)
                instance = ContextCompat.getSystemService(context, ActivityManager::class.java)
                    ?: throw NullPointerException()

            return instance!!
        }
    }

    private val packageManager = context.packageManager

    fun getLastApp(): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            getLollipopMr1LastApp()
        else
            getLollipopLastApp()
    }

    private fun getLollipopLastApp(): String? {
        val tasks = try {
            activityManager.get()
                .getRecentTasks(1, ActivityManager.RECENT_IGNORE_UNAVAILABLE)
        } catch (e: SecurityException) {
            emptyList<ActivityManager.RecentTaskInfo>()
        }

        if (tasks.isNotEmpty()) {
            return tasks[0].baseIntent.component?.packageName
        }
        return null
    }

    private fun getLollipopMr1LastApp(): String? {
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