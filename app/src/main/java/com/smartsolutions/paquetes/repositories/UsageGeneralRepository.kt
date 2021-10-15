package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IUsageGeneralDao
import com.smartsolutions.paquetes.repositories.contracts.IUsageGeneralRepository
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import javax.inject.Inject

class UsageGeneralRepository @Inject constructor(
    private val dao: IUsageGeneralDao
) : IUsageGeneralRepository {

    override suspend fun all(): List<UsageGeneral>{
        return dao.getAll()
    }

    override suspend fun inRangeTime(start: Long, finish: Long): List<UsageGeneral>{
        return dao.inRangeTime(start, finish)
    }

    override suspend fun create(vararg usageGeneral: UsageGeneral){
        dao.create(usageGeneral.asList())
    }

    override suspend fun update(vararg usageGeneral: UsageGeneral){
        dao.update(usageGeneral.asList())
    }

    override suspend fun delete(vararg usageGeneral: UsageGeneral){
        dao.delete(usageGeneral.asList())
    }
}