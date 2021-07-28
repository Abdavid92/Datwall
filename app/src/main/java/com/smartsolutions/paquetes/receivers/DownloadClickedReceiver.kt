package com.smartsolutions.paquetes.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.smartsolutions.paquetes.managers.UpdateManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.ui.ACTION_OPEN_FRAGMENT
import com.smartsolutions.paquetes.ui.EXTRA_FRAGMENT
import com.smartsolutions.paquetes.ui.FRAGMENT_UPDATE_DIALOG
import com.smartsolutions.paquetes.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DownloadClickedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateManager: IUpdateManager

    override fun onReceive(context: Context, intent: Intent) {
        intent.getLongArrayExtra(DownloadManager.EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS)?.let { ids ->
            updateManager.getSavedDownloadId()?.let {
                if (ids.contains(it)){
                    context.startActivity(Intent(context, MainActivity::class.java).apply {
                        action = ACTION_OPEN_FRAGMENT
                        putExtra(EXTRA_FRAGMENT, FRAGMENT_UPDATE_DIALOG)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        }
    }
}