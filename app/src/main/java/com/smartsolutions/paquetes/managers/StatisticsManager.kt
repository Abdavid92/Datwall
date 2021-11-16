package com.smartsolutions.paquetes.managers

import android.content.Context
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IStatisticsManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.apache.commons.lang.time.DateUtils
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

        var quantity = timeUnit.convert(finish - start, TimeUnit.MILLISECONDS)

        if (quantity < 1L)
            quantity = 1

        return if (usage.totalBytes.bytes > 0) {
            DataUnitBytes(usage.totalBytes.bytes / quantity)
        } else {
            DataUnitBytes(0L)
        }
    }

    override suspend fun getRemainder(timeUnit: TimeUnit): DataUnitBytes {
        simManager.getDefaultSim(SimDelegate.SimType.DATA)?.let { sim ->
            return getRemainder(timeUnit, userDataBytesRepository.bySimId(sim.id))
        }

        return DataUnitBytes(0L)
    }

    override suspend fun getRemainder(
        timeUnit: TimeUnit,
        userData: List<UserDataBytes>
    ): DataUnitBytes {
        if (timeUnit.ordinal < TimeUnit.HOURS.ordinal)
            throw IllegalArgumentException()

        val enabledLte = withContext(Dispatchers.IO) {
            context.internalDataStore.data.firstOrNull()
                ?.get(PreferencesKeys.ENABLED_LTE) ?: false
        }

        val list = if (enabledLte) {
            userData.filter {
                it.type != DataBytes.DataType.National &&
                        it.type != DataBytes.DataType.MessagingBag &&
                        !it.isExpired() &&
                        it.exists()
            }
        } else {
            userData.filter {
                (it.type == DataBytes.DataType.International ||
                        it.type == DataBytes.DataType.PromoBonus) &&
                        !it.isExpired() &&
                        it.exists()
            }
        }

        if (list.isNotEmpty()) {

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
                dateExpired = DateUtils.addDays(date, DataPackages.GENERAL_DURATION).time
            } else if (dateExpired == 0L && !containPackages) {
                val dailyBag =
                    DataPackages.PACKAGES.first { it.id == DataPackages.PackageId.DailyBag }
                dateExpired = DateUtils.addDays(date, dailyBag.duration).time
            }

            var quantity = timeUnit.convert(dateExpired - date.time, TimeUnit.MILLISECONDS)

            if (quantity < 1L)
                quantity = 1

            return DataUnitBytes(bytes / quantity)
        }

        return DataUnitBytes(0L)
    }

}