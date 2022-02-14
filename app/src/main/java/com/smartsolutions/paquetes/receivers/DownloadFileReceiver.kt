package com.smartsolutions.paquetes.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class DownloadFileReceiver : BroadcastReceiver() {

    @Inject
    lateinit var updateManager: IUpdateManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        runBlocking {

            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            if (id == updateManager.getSavedDownloadId()) {

                if (updateManager.isDownloaded(id)) {

                    notificationHelper.notifyUpdate(
                        "Actualización Descargada",
                        "Toque aquí para instalar la actualización"
                    )
                } else {

                    notificationHelper.notifyUpdate(
                        "Actualización Interrumpida",
                        "Toque aquí para ver el estado de la actualización"
                    )
                }
            }
        }

    }
}