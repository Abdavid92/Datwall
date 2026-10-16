package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AppsManager @Inject constructor(
    private val appRepository: IAppRepository
) : IAppsManager {

    override fun flowApps() = appRepository.flowByGroup()

    override fun sections(apps: List<IApp>, filter: AppsFilter) = AppListRules.sections(apps, filter)

    override fun search(apps: List<IApp>, query: String?) = AppListRules.search(apps, query)

    override suspend fun all() = withContext(Dispatchers.IO) {
        appRepository.all()
    }

    override suspend fun byUids(uids: IntArray) = withContext(Dispatchers.IO) {
        appRepository.get(uids)
    }

    override suspend fun commit(apps: List<IApp>) {
        withContext(Dispatchers.IO) {
            appRepository.update(apps)
        }
    }
}
