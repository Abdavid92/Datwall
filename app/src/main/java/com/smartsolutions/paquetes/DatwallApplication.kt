package com.smartsolutions.paquetes

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.smartsolutions.datwall.managers.NetworkUsageDigger
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.watcher.PackageMonitor
import com.smartsolutions.datwall.watcher.Watcher
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DatwallApplication : Application() {

    @Inject
    lateinit var watcher: Watcher

    @Inject
    lateinit var packageMonitor: PackageMonitor

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            packageMonitor.forceSynchronization {
                watcher.start()
            }
        }

    }

    override fun onTerminate() {
        watcher.stop()
        super.onTerminate()
    }
}