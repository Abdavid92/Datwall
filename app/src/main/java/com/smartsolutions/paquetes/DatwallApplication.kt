package com.smartsolutions.paquetes

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

    private val TAG = "Watcher"

    override fun onCreate() {
        super.onCreate()

        GlobalScope.launch {
            packageMonitor.forceSynchronization {
                watcher.start()
            }
        }

        val receiver = object : BroadcastReceiver() {

            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {

                    val app = intent.getParcelableExtra<App>(Watcher.EXTRA_FOREGROUND_APP)

                    Log.i(TAG, "La app ${app?.packageName} está en primer plano")
                }
            }
        }

        val filter = IntentFilter(Watcher.ACTION_CHANGE_APP_FOREGROUND)

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, filter)
    }

    override fun onTerminate() {
        watcher.stop()

        super.onTerminate()
    }
}