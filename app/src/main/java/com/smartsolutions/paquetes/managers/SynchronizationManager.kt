package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.work.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.*
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.workers.SynchronizationWorker
import com.smartsolutions.paquetes.workersDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SynchronizationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper,
    private val simRepository: ISimRepository
) : ISynchronizationManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private var _synchronizationMode = IDataPackageManager.ConnectionMode.USSD
    override var synchronizationMode: IDataPackageManager.ConnectionMode
        get() = _synchronizationMode
        set(value) {
            if (value == IDataPackageManager.ConnectionMode.Unknown)
                return

            launch(Dispatchers.IO) {
                context.settingsDataStore.edit {
                    it[PreferencesKeys.SYNCHRONIZATION_MODE] = value.name
                }
            }
            _synchronizationMode = value
        }

    private var _synchronizationUSSDModeModern: Boolean = true
    override var synchronizationUSSDModeModern: Boolean
        get() = _synchronizationUSSDModeModern
        set(value) {
            _synchronizationUSSDModeModern = value
            launch {
                context.workersDataStore.edit {
                    it[PreferencesKeys.SYNCHRONIZATION_USSD_MODE_MODERN] = value
                }
            }
        }

    init {
        launch(Dispatchers.IO) {
            context.settingsDataStore.data.collect {
                _synchronizationMode = IDataPackageManager.ConnectionMode
                    .valueOf(it[PreferencesKeys.SYNCHRONIZATION_MODE] ?: _synchronizationMode.name)
            }
        }
        launch(Dispatchers.IO) {
            context.workersDataStore.data.collect {
                _synchronizationUSSDModeModern =
                    it[PreferencesKeys.SYNCHRONIZATION_USSD_MODE_MODERN] ?: true
            }
        }
    }

    override suspend fun synchronizeUserDataBytes(sim: Sim) {
        if (_synchronizationMode != IDataPackageManager.ConnectionMode.USSD)
            return

        val data = if (sim.network == Networks.NETWORK_NONE) {
            // Las líneas empresariales consultan datos, minutos y sms con *111#.
            val balance = sendUSSDRequest(BUSINESS_BALANCE_USSD, sim) ?: return
            checkMmi(balance)
            UssdBalanceParser.parsePackages(joinString(balance))
        } else {
            val bytesPackages = sendUSSDRequest(BALANCE_USSD, sim) ?: return
            val bonusPackages = sendUSSDRequest(BONUS_USSD, sim) ?: return
            checkMmi(bytesPackages)
            checkMmi(bonusPackages)
            UssdBalanceParser.parsePackages(joinString(bytesPackages)) +
                    UssdBalanceParser.parseBonus(joinString(bonusPackages))
        }

        userDataBytesManager.synchronizeUserDataBytes(fillMissingDataBytes(data), sim.id)
        withContext(Dispatchers.IO) {
            simRepository.update(sim.apply {
                lastSynchronization = System.currentTimeMillis()
            })
        }
    }

    private fun checkMmi(response: Array<CharSequence>) {
        if (response.any { it.contains("mmi", true) })
            throw USSDRequestException(
                USSDHelper.USSD_CODE_FAILED,
                USSDHelper.USSD_MMI_FULL
            )
    }

    override fun scheduleUserDataBytesSynchronization(intervalInMinutes: Int) {
        if (intervalInMinutes < 15 || intervalInMinutes > 120 || Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return

        val workRequest = PeriodicWorkRequestBuilder<SynchronizationWorker>(
            intervalInMinutes.toLong(),
            TimeUnit.MINUTES
        )
            .addTag(SYNCHRONIZATION_WORKER_TAG)
            .build()

        val workManager = WorkManager.getInstance(context)

        workManager.cancelAllWorkByTag(SYNCHRONIZATION_WORKER_TAG)
        workManager.enqueue(workRequest)
    }

    override fun cancelScheduleUserDataBytesSynchronization() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(SYNCHRONIZATION_WORKER_TAG)
    }

    private fun fillMissingDataBytes(data: List<DataBytes>): List<DataBytes> {
        val list = data.toMutableList()

        DataBytes.DataType.values().forEach { type ->
            if (list.firstOrNull { it.type == type } == null) {
                list.add(DataBytes(type, 0, 0))
            }
        }
        return list
    }

    private fun joinString(charSequence: Array<CharSequence>): String {
        var string = ""
        charSequence.forEach {
            string += it.toString()
        }
        return string
    }

    private suspend fun sendUSSDRequest(ussd: String, sim: Sim): Array<CharSequence>? {
        return if (_synchronizationUSSDModeModern) {
            ussdHelper.sendUSSDRequest(ussd, sim)
        } else {
            ussdHelper.sendUSSDRequestLegacy(ussd, true, sim)
        }
    }

    companion object {
        const val BALANCE_USSD = "*222*328#"
        const val BONUS_USSD = "*222*266#"
        const val BUSINESS_BALANCE_USSD = "*111#"

        const val SYNCHRONIZATION_WORKER_TAG = "synchronization_worker"
    }
}