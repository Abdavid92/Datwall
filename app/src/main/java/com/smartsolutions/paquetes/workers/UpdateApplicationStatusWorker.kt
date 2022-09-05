package com.smartsolutions.paquetes.workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abdavid92.persistentlog.Log
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
    private val updateManager: IUpdateManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        runCatching {

            updateManager.findUpdate()?.let {

                val nm = NotificationManagerCompat.from(applicationContext)

                val intent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    Intent(Intent.ACTION_VIEW, it.uri),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else
                        PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(
                    applicationContext,
                    NotificationHelper.ALERT_CHANNEL_ID
                ).setContentTitle("Nueva actualización disponible")
                    .setContentText("Versión: ${it.version}. Toque aquí para actualizar.")
                    .setContentIntent(intent)
                    .build()

                nm.notify(NotificationHelper.ALERT_NOTIFICATION_ID, notification)
            }

            Log.i("Update Worker", "Launched")
        }

        return Result.success()
    }
}