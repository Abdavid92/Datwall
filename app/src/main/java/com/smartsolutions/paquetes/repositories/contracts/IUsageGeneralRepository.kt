package com.smartsolutions.paquetes.repositories.contracts

import com.smartsolutions.paquetes.repositories.models.UsageGeneral

interface IUsageGeneralRepository {
    suspend fun all(): List<UsageGeneral>
    suspend fun inRangeTime(start: Long, finish: Long): List<UsageGeneral>
    suspend fun create(vararg usageGeneral: UsageGeneral)
    suspend fun update(vararg usageGeneral: UsageGeneral)
    suspend fun delete(vararg usageGeneral: UsageGeneral)
}