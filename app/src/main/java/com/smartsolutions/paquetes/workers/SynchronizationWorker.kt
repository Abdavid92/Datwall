package com.smartsolutions.paquetes.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.watcher.RxWatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull

@HiltWorker
class SynchronizationWorker @AssistedInject constructor(
    @Assisted
    private val context: Context,
    @Assisted
    params: WorkerParameters,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val synchronizationManager: ISynchronizationManager,
    private val simManager: ISimManager
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        var canExecute = true

        val onlyInternational = context.dataStore.data.firstOrNull()
            ?.get(PreferencesKeys.SYNCHRONIZATION_ONLY_INTERNATIONAL) ?: true
        val onlyDummy =
            context.dataStore.data.firstOrNull()?.get(PreferencesKeys.SYNCHRONIZATION_ONLY_DUMMY)
                ?: true

        if (onlyInternational) {
            try {
                canExecute = userDataBytesRepository.get(
                    simManager.getDefaultSim(SimDelegate.SimType.DATA).id,
                    DataBytes.DataType.International
                ).exists()
            } catch (e: Exception) {
            }
        }

        if (onlyDummy && canExecute) {
            canExecute =
                (RxWatcher.lastRxBytes + RxWatcher.lastTxBytes) <= (5L * 1000L) && RxWatcher.lastRxBytes >= 0 && RxWatcher.lastTxBytes >= 0
        }

        if (canExecute) {
            try {
                Log.i("SYNCHRONIZATION", "Start Sincro $canExecute")
                synchronizationManager.synchronizeUserDataBytes(simManager.getDefaultSim(SimDelegate.SimType.VOICE))
            } catch (e: Exception) {
            }
        }

        Log.i("SYNCHRONIZATION", "Sincronizado $canExecute")

        return Result.success()
    }
}