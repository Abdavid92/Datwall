package com.smartsolutions.paquetes.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext


@HiltWorker
class UpdateApplicationStatusWorker @AssistedInject constructor(
    @Assisted
    private val context: Context,
    @Assisted
    params: WorkerParameters,
    private val updateManager: IUpdateManager,
    private val notificationHelper: NotificationHelper
) : Worker(context, params), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override fun doWork(): Result {
        launch {
            updateManager.findUpdate()?.let { androidApp ->
                val isAutoUpdate =
                    context.dataStore.data.firstOrNull()?.get(PreferencesKeys.AUTO_UPDATE) ?: false
                val url = Uri.parse(
                    updateManager.buildDynamicUrl(
                        updateManager.BASE_URL_APKLIS,
                        androidApp
                    )
                )

                if (isAutoUpdate) {
                    if (updateManager.foundIfDownloaded(url) == null) {
                        updateManager.downloadUpdate(url)
                    } else {
                        notificationHelper.notifyUpdate(
                            "Actualización Lista",
                            "Presione aquí para ir a instalar la actualización"
                        )
                    }
                }else {
                    notificationHelper.notifyUpdate(
                        "Actualización Disponible",
                        "Presione aquí para descargar la actualización"
                    )
                }
            }
        }

        return Result.success()
    }
}