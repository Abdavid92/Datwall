package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.TrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import javax.inject.Inject

class NetworkUsageManagerLegacy @Inject constructor(
    private val trafficRepository: TrafficRepository
): NetworkUsageManager() {

    override suspend fun getAppUsage(uid: Int, start: Long, finish: Long): Traffic {
        val traffic = Traffic(uid, 0L, 0L)
        traffic.startTime = start
        traffic.endTime = finish
        trafficRepository.getByUid(uid, start, finish).forEach {
            traffic += it
        }
        return traffic
    }

    override suspend fun getAppsUsage(start: Long, finish: Long): List<Traffic> {
        return trafficRepository.getByTime(start, finish)
    }

    override suspend fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long) {
        apps.forEach { iapp ->
            if (iapp is App)
                iapp.traffic = getAppUsage(iapp.uid, start, finish)
            else if (iapp is AppGroup)
                iapp.forEach { app ->
                    app.traffic = getAppUsage(app.uid, start, finish)
                }
        }
    }

    override suspend fun getUsageTotal(start: Long, finish: Long): Traffic {
        val traffic = Traffic(0, 0L, 0L)
        traffic.startTime = start
        traffic.endTime = finish
        trafficRepository.getByTime(start, finish).forEach {
            traffic += it
        }
        return traffic
    }
}