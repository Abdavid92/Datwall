package com.smartsolutions.paquetes

import android.content.Context
import android.net.TrafficStats
import android.os.Build
import androidx.work.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TrafficRegistration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficRepository: ITrafficRepository,
    private val networkUtil: NetworkUtil,
    private val simManager: ISimManager,
    private val appRepository: IAppRepository
) {

    fun startRegistration(intervalInMilliseconds: Long){
        val request = PeriodicWorkRequestBuilder<TrafficRegistrationWorker>(intervalInMilliseconds, TimeUnit.MILLISECONDS)
        request.addTag(TRAFFIC_REGISTRATION_TAG)
        WorkManager.getInstance(context).enqueue(request.build())
    }

    fun stopRegistration(){
        WorkManager.getInstance(context).cancelAllWorkByTag(TRAFFIC_REGISTRATION_TAG)
    }


    suspend fun obtainTraffic(){
        val simID = simManager.getDefaultDataSim().id
        val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G
        val trafficsToAdd = mutableListOf<Traffic>()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            appRepository.all().forEach {
                processTraffic(it.uid, simID, TrafficStats.getUidRxBytes(it.uid), TrafficStats.getUidTxBytes(it.uid), isLte)?.let {
                    trafficsToAdd.add(it)
                }
            }
        }

        processTraffic(GENERAL_TRAFFIC_UID, simID, TrafficStats.getTotalRxBytes(), TrafficStats.getTotalTxBytes(), isLte)?.let {
            trafficsToAdd.add(it)
        }

        trafficRepository.create(trafficsToAdd)
    }

    private fun processTraffic(uid: Int, simID: String, rxBytes: Long, txBytes: Long, isLte: Boolean): Traffic? {
        var oldTraffic = traffics.firstOrNull{ it.uid == uid }

        if (oldTraffic == null){
            oldTraffic = Traffic(uid, rxBytes, txBytes, simID)
            oldTraffic.endTime = System.currentTimeMillis()
            traffics.add(oldTraffic)
        }else {
            val traffic = Traffic(uid, (rxBytes - oldTraffic.rxBytes.bytes), (txBytes - oldTraffic.txBytes.bytes), simID)

            traffic.startTime = oldTraffic.endTime
            traffic.endTime = System.currentTimeMillis()

            if (isLte){
                traffic.network = Networks.NETWORK_4G
            }else {
                traffic.network = Networks.NETWORK_3G
            }
            return traffic
        }

        return null
    }


    inner class TrafficRegistrationWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

        override fun doWork(): Result {
            TODO("Not yet implemented")
        }

    }

    companion object {
        const val TRAFFIC_REGISTRATION_TAG = "traffic_registration_tag"
        const val GENERAL_TRAFFIC_UID = Int.MIN_VALUE

        private var traffics = mutableListOf<Traffic>()
    }

}