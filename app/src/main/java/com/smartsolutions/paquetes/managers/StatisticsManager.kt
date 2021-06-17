package com.smartsolutions.paquetes.managers

import android.content.Context
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IStatisticsManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.micubacel.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import org.apache.commons.lang3.time.DateUtils
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class StatisticsManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val networkUsageManager: NetworkUsageManager,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val simManager: ISimManager
) : IStatisticsManager {

    override suspend fun getAverage(start: Long, finish: Long, timeUnit: TimeUnit): DataUnitBytes {
        if (timeUnit.ordinal < TimeUnit.HOURS.ordinal)
            throw IllegalArgumentException()

        if (start >= finish)
            throw IllegalArgumentException()

        val usage = networkUsageManager.getUsageTotal(start, finish)

        var quantity = timeUnit.convert(finish - start, timeUnit)

        if (quantity < 1L)
            quantity = 1

        return DataUnitBytes(usage.totalBytes.bytes / quantity)
    }

    override suspend fun getRemainder(timeUnit: TimeUnit): DataUnitBytes {
        if (timeUnit.ordinal < TimeUnit.HOURS.ordinal)
            throw IllegalArgumentException()

        val sim = simManager.getDefaultDataSim()

        val enabledLte = context.dataStore.data.firstOrNull()
            ?.get(PreferencesKeys.ENABLED_LTE) ?: false

        val list = if (enabledLte) {
            userDataBytesRepository.bySimId(sim.id)
                .filter { it.type != DataBytes.DataType.National &&
                        !it.isExpired()
                }
        } else {
            userDataBytesRepository.bySimId(sim.id)
                .filter {
                    it.type == DataBytes.DataType.International &&
                    it.type == DataBytes.DataType.PromoBonus
                    !it.isExpired()
                }
        }

        var dateExpired = 0L
        var bytes = 0L

        var containPackages = false

        list.forEach {
            if (it.expiredTime > dateExpired)
                dateExpired = it.expiredTime

            bytes += it.bytes

            if (it.type != DataBytes.DataType.DailyBag)
                containPackages = true
        }

        val date = Date()

        if (dateExpired == 0L && containPackages) {
            dateExpired = DateUtils.addDays(date, DataPackagesContract.GENERAL_DURATION).time
        } else if (dateExpired == 0L && !containPackages) {
            dateExpired = DateUtils.addDays(date, DataPackagesContract.DailyBag.duration).time
        }

        var quantity = timeUnit.convert(dateExpired - date.time, timeUnit)

        if (quantity < 1L)
            quantity = 1

        return DataUnitBytes(bytes / quantity)
    }

}