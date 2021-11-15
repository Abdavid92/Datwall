package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import java.util.concurrent.TimeUnit

interface IStatisticsManager {

    suspend fun getAverage(start: Long, finish: Long, timeUnit: TimeUnit): DataUnitBytes

    suspend fun getRemainder(timeUnit: TimeUnit): DataUnitBytes

    suspend fun getRemainder(timeUnit: TimeUnit, userData: List<UserDataBytes>): DataUnitBytes
}