package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import androidx.work.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.helpers.getBytesFromText
import com.smartsolutions.paquetes.managers.contracts.*
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.workers.SynchronizationWorker
import com.smartsolutions.paquetes.workersDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import org.apache.commons.lang.time.DateUtils
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class SynchronizationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper,
    private val simManager: ISimManager,
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
        if (_synchronizationMode == IDataPackageManager.ConnectionMode.USSD) {
            val data = mutableListOf<DataBytes>()

            val bytesPackages = sendUSSDRequest("*222*328#")
            val bonusPackages = sendUSSDRequest("*222*266#")

            if (bytesPackages != null && bonusPackages != null) {

                bytesPackages.forEach {
                    if (it.contains("mmi", true))
                        throw USSDRequestException(
                            USSDHelper.USSD_CODE_FAILED,
                            USSDHelper.USSD_MMI_FULL
                        )
                }

                bonusPackages.forEach {
                    if (it.contains("mmi", true))
                        throw USSDRequestException(
                            USSDHelper.USSD_CODE_FAILED,
                            USSDHelper.USSD_MMI_FULL
                        )
                }

                data.addAll(obtainDataBytesPackages(bytesPackages))
                data.addAll(obtainDataByteBonus(bonusPackages))

                simManager.getDefaultSim(SimDelegate.SimType.VOICE)?.let {
                    userDataBytesManager.synchronizeUserDataBytes(
                        fillMissingDataBytes(data),
                        it.id
                    )
                    withContext(Dispatchers.IO) {
                        simRepository.update(it.apply {
                            lastSynchronization = System.currentTimeMillis()
                        })
                    }
                }

            }
        }
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

    private fun obtainDataBytesPackages(bytesPackages: Array<CharSequence>): Collection<DataBytes> {
        val response = joinString(bytesPackages)

        val data = mutableListOf<DataBytes>()

        if (response.contains(PAQUETES))
            data.addAll(getInternationals(response))

        val value = getBytesFromText(DIARIA, response)
        if (value > 0) {
            data.add(
                DataBytes(
                    DataBytes.DataType.DailyBag,
                    value,
                    getExpireDatePackages(response, true)
                )
            )
        }

        return data
    }

    private fun obtainDataByteBonus(bonusPackages: Array<CharSequence>): Collection<DataBytes> {
        val response = joinString(bonusPackages)

        val data = mutableListOf<DataBytes>()

        var value = getBytesFromText(PROMO_BONO, response)
        if (value >= 0) {
            data.add(
                DataBytes(
                    DataBytes.DataType.PromoBonus,
                    value,
                    getExpireDateBonus(PROMO_BONO, response)
                )
            )
        }

        value = getBytesFromText(NATIONAL, response)
        if (value >= 0) {
            data.add(
                DataBytes(
                    DataBytes.DataType.National,
                    value,
                    getExpireDateBonus(NATIONAL, response)
                )
            )
        }

        return data
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

    private fun getInternationals(text: String): List<DataBytes> {
        var international = 0L
        var internationalLte = 0L

        if (!text.contains(LTE)) {

            international = getBytesFromText(PAQUETES, text)

        } else if (text.contains(LTE) && !text.contains(PLUS_SYMBOL)) {

            internationalLte = getBytesFromText(PAQUETES, text)

        } else if (text.contains(LTE) && text.contains(PLUS_SYMBOL)) {

            international = getBytesFromText(PAQUETES, text)
            internationalLte = getBytesFromText(PLUS_SYMBOL, text)
        }

        return listOf(
            DataBytes(
                DataBytes.DataType.International,
                international,
                getExpireDatePackages(text, false)
            ),
            DataBytes(
                DataBytes.DataType.InternationalLte,
                internationalLte,
                getExpireDatePackages(text, false)
            )
        )
    }

    private fun getExpireDatePackages(text: String, isBolsa: Boolean): Long {
        if (isBolsa && !text.contains(DIARIA) || !isBolsa && !text.contains(PAQUETES)) {
            return 0L
        }

        val start = if (isBolsa) {
            text.indexOf("validos", text.indexOf(DIARIA)) + 7
        } else {
            text.indexOf("validos", text.indexOf(PAQUETES)) + 7
        }

        val finish = if (isBolsa) {
            text.indexOf("horas", start)
        } else {
            text.indexOf("dias", start)
        }

        return try {
            val value = text.substring(start, finish).trimStart().trimEnd().toInt()
            if (isBolsa) {
                DateUtils.addHours(Date(), value).time
            } else {
                var date = DateUtils.addDays(Date(), value)
                date = DateUtils.setHours(date, 23)
                date = DateUtils.setMinutes(date, 59)
                date = DateUtils.setSeconds(date, 59)
                date.time
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun getExpireDateBonus(find: String, text: String): Long {
        if (!text.contains(find)) {
            return 0L
        }
        val start = text.indexOf("->", text.indexOf(find)) + 2
        val finish = text.indexOf(".", start)

        return try {
            val dateString = text.substring(start, finish).trimStart().trimEnd()
            var date = SimpleDateFormat("dd-MM-yy", Locale.US).parse(dateString)
            date = DateUtils.setHours(date, 23)
            date = DateUtils.setMinutes(date, 59)
            date = DateUtils.setSeconds(date, 59)
            date?.time ?: 0
        } catch (e: Exception) {
            0L
        }
    }

    private fun joinString(charSequence: Array<CharSequence>): String {
        var string = ""
        charSequence.forEach {
            string += it.toString()
        }
        return string
    }

    private suspend fun sendUSSDRequest(ussd: String): Array<CharSequence>? {
        return if (_synchronizationUSSDModeModern) {
            ussdHelper.sendUSSDRequest(ussd)
        } else {
            ussdHelper.sendUSSDRequestLegacy(ussd, true)
        }
    }

    companion object {
        const val DIARIA = "Diaria:"
        const val PAQUETES = "Paquetes:"
        const val PROMO_BONO = "Datos"
        const val NATIONAL = "Datos.cu"
        const val PLUS_SYMBOL = "+"
        const val LTE = "LTE"

        const val SYNCHRONIZATION_WORKER_TAG = "synchronization_worker"
    }
}