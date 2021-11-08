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
import com.smartsolutions.paquetes.repositories.EventRepository
import com.smartsolutions.paquetes.repositories.IEventRepository
import com.smartsolutions.paquetes.repositories.models.Event
import com.smartsolutions.paquetes.ui.update.UpdateViewModel
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
    private val notificationHelper: NotificationHelper,
    private val eventRepository: IEventRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        runCatching {

            updateManager.findUpdate()?.let { androidApp ->
                val isAutoUpdate = withContext(Dispatchers.IO) {
                    applicationContext.settingsDataStore.data
                        .firstOrNull()
                        ?.get(PreferencesKeys.AUTO_UPDATE) ?: false
                }

                if (isAutoUpdate) {

                    val mode = IUpdateManager.UpdateMode.valueOf(
                        applicationContext.settingsDataStore.data.firstOrNull()
                            ?.get(PreferencesKeys.UPDATE_MODE)
                            ?: IUpdateManager.UpdateMode.APKLIS_SERVER.name
                    )

                    val url = when (mode) {
                        IUpdateManager.UpdateMode.APKLIS_SERVER -> {
                            Uri.parse(
                                updateManager.buildDynamicUrl(
                                    updateManager.BASE_URL_APKLIS,
                                    androidApp
                                )
                            )
                        }
                        else -> {
                            Uri.parse(
                                updateManager.buildDynamicUrl(
                                    updateManager.BASE_URL_HOSTINGER,
                                    androidApp
                                )
                            )
                        }
                    }

                    if (updateManager.foundIfDownloaded(url) == null) {
                        updateManager.downloadUpdate(url)
                        notificationHelper.notifyUpdate(
                            "Descargando Actualización",
                            "Presione aquí para ver el progreso de la actualización"
                        )
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

            eventRepository.create(
                Event(
                    System.currentTimeMillis(),
                    Event.EventType.INFO,
                    "Update Worker",
                    "Launched"
                )
            )
        }

        return Result.success()
    }
}