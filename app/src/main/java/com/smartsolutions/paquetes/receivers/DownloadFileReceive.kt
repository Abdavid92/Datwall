package com.smartsolutions.paquetes.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.UpdateManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class DownloadFileReceive : BroadcastReceiver() {

    @Inject
    lateinit var updateManager: IUpdateManager

    override fun onReceive(context: Context, intent: Intent) {
        runBlocking {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == updateManager.getSavedDownloadId()){
                updateManager.installDownloadedPackage(id)
            }
        }



    }
}