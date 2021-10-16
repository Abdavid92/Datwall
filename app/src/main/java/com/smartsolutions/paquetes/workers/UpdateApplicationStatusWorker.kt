package com.smartsolutions.paquetes.workers

import android.content.Context
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext


@HiltWorker
class UpdateApplicationStatusWorker @AssistedInject constructor(
    @Assisted
    context: Context,
    @Assisted
    params: WorkerParameters,
    private val updateManager: IUpdateManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        updateManager.findUpdate()?.let { androidApp ->
            val isAutoUpdate = withContext(Dispatchers.IO){
                applicationContext.settingsDataStore.data
                    .firstOrNull()
                    ?.get(PreferencesKeys.AUTO_UPDATE) ?: false
            }

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
            } else {
                notificationHelper.notifyUpdate(
                    "Actualización Disponible",
                    "Presione aquí para descargar la actualización"
                )
            }
        }

        return Result.success()
    }
}