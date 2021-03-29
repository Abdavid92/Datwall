package com.smartsolutions.datwall.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsolutions.datwall.watcher.ChangeType
import com.smartsolutions.datwall.watcher.PackageMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class PackageMonitorReceiver : BroadcastReceiver(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    @Inject
    lateinit var packageMonitor: PackageMonitor

    override fun onReceive(context: Context, intent: Intent) {

        val packageName = intent.data?.encodedSchemeSpecificPart

        packageName?.let {

            val changeType: ChangeType = when (intent.action) {
                Intent.ACTION_PACKAGE_ADDED -> ChangeType.Created
                Intent.ACTION_PACKAGE_REPLACED -> ChangeType.Updated
                Intent.ACTION_PACKAGE_FULLY_REMOVED -> ChangeType.Deleted
                else -> ChangeType.None
            }

            launch {
                packageMonitor.synchronizeDatabase(it, changeType)
                TODO("Reiniciar el vpn en algunos casos")
            }
        }
    }
}