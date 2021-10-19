package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.data.IUsageGeneralDao
import com.smartsolutions.paquetes.repositories.contracts.IUsageGeneralRepository
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UsageGeneralRepository @Inject constructor(
    private val dao: IUsageGeneralDao
) : IUsageGeneralRepository {

    private val dispatcher = Dispatchers.IO

    override suspend fun all(): List<UsageGeneral>{
        return withContext(dispatcher){
            dao.getAll()
        }
    }

    override suspend fun inRangeTime(start: Long, finish: Long): List<UsageGeneral>{
        return withContext(dispatcher){
            dao.inRangeTime(start, finish)
        }
    }

    override suspend fun create(vararg usageGeneral: UsageGeneral){
        withContext(dispatcher){
            dao.create(usageGeneral.asList())
        }
    }

    override suspend fun update(vararg usageGeneral: UsageGeneral){
        withContext(dispatcher){
            dao.update(usageGeneral.asList())
        }
    }

    override suspend fun delete(vararg usageGeneral: UsageGeneral){
        withContext(dispatcher){
            dao.delete(usageGeneral.asList())
        }
    }
}