package com.smartsolutions.paquetes.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

private const val NOTIFICATION_ID = 88

@HiltWorker
class SynchronizationWorker @AssistedInject constructor(
    @Assisted
    private val context: Context,
    @Assisted
    params: WorkerParameters,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val synchronizationManager: ISynchronizationManager,
    private val simManager: ISimManager,
    private val notificationHelper: NotificationHelper
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        var canExecute = true

        try {
            canExecute = userDataBytesRepository.get(
                simManager.getDefaultSim(SimDelegate.SimType.DATA).id,
                DataBytes.DataType.International
            ).exists()
        } catch (e: Exception) { }

        if (canExecute) {
            canExecute =
                (RxWatcher.lastRxBytes + RxWatcher.lastTxBytes) <= (2 * 1000) && RxWatcher.lastRxBytes >= 0 && RxWatcher.lastTxBytes >= 0
        }

        if (canExecute) {
            notifyUpdate()
            try {
                Log.i("SYNCHRONIZATION", "Start Sincro $canExecute")
                synchronizationManager.synchronizeUserDataBytes(simManager.getDefaultSim(SimDelegate.SimType.VOICE))
            } catch (e: Exception) {
            }
            cancelNotification()
        }

        Log.i("SYNCHRONIZATION", "Sincronizado $canExecute")

        return Result.success()
    }

    private fun notifyUpdate() {
        notificationHelper.notify(
            NOTIFICATION_ID,
            notificationHelper.buildNotification(
                NotificationHelper.WORKERS_CHANNEL_ID,
                R.drawable.ic_synchronization_notification
            )
                .setOngoing(true)
                .setContentTitle(context.getString(R.string.notification_synchronization_title))
                .setContentText(context.getString(R.string.notification_synchronization_description))
                .build()
        )
    }

    private fun cancelNotification() {
        notificationHelper.cancelNotification(NOTIFICATION_ID)
    }
}