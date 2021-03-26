package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class PackageMonitorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
            Toast.makeText(context, "package added", Toast.LENGTH_SHORT).show()
        } else if (intent.action == Intent.ACTION_PACKAGE_REPLACED) {
            Toast.makeText(context, "package replaced", Toast.LENGTH_SHORT).show()
        } else if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
            Toast.makeText(context, "package removed", Toast.LENGTH_SHORT).show()
        }
    }
}