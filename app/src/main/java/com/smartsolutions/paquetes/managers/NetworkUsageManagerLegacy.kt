package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.receivers.TrafficRegistrationReceiver
import com.smartsolutions.paquetes.repositories.TrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import javax.inject.Inject

class NetworkUsageManagerLegacy @Inject constructor(
    private val trafficRepository: TrafficRepository,
    simManager: ISimManager
): NetworkUsageManager(simManager) {

    override suspend fun getAppUsage(uid: Int, start: Long, finish: Long, updateSim: Boolean): Traffic {
        if (updateSim) {
            updateSimID()
        }
        val traffic = Traffic(uid, 0L, 0L, simId)
        traffic.startTime = start
        traffic.endTime = finish
        trafficRepository.getByUid(uid, simId, start, finish).forEach {
            traffic += it
        }
        return traffic
    }

    override suspend fun getAppsUsage(start: Long, finish: Long): List<Traffic> {
        updateSimID()
        return trafficRepository.getByTime(simId, start, finish)
    }

    override suspend fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long) {
        updateSimID()
        apps.forEach { iapp ->
            if (iapp is App)
                iapp.traffic = getAppUsage(iapp.uid, start, finish, false)
            else if (iapp is AppGroup)
                iapp.forEach { app ->
                    app.traffic = getAppUsage(app.uid, start, finish, false)
                }
        }
    }

    override suspend fun getUsageTotal(start: Long, finish: Long): Traffic {
        updateSimID()
        val traffic = Traffic(0, 0L, 0L, simId)
        traffic.startTime = start
        traffic.endTime = finish
        trafficRepository.getByUid(TrafficRegistrationReceiver.GENERAL_TRAFFIC_UID, simId, start, finish).forEach {
            traffic += it
        }
        /*trafficRepository.getByTime(simId, start, finish).forEach {
            traffic += it
        }*/
        return traffic
    }
}