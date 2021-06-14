package com.smartsolutions.paquetes

import android.content.Context
import androidx.work.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.managers.NetworkUtils
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.TrafficRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TrafficRegistration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficRepository: ITrafficRepository,
    private val networkUtil: NetworkUtil,
    private val simManager: ISimManager
) {

    fun startRegistration(intervalInMilliseconds: Long){
        val request = PeriodicWorkRequestBuilder<TrafficRegistrationWorker>(intervalInMilliseconds, TimeUnit.MILLISECONDS)
        request.addTag(TRAFFIC_REGISTRATION_TAG)
        WorkManager.getInstance(context).enqueue(request.build())
    }

    fun stopRegistration(){
        WorkManager.getInstance(context).cancelAllWorkByTag(TRAFFIC_REGISTRATION_TAG)
    }



    private suspend fun registerTraffic(uid: Int, rxBytes: Long, txBytes: Long) {
        var oldTraffic = traffics.firstOrNull{ it.uid == uid }

        if (oldTraffic == null){
            oldTraffic = Traffic(uid, rxBytes, txBytes)
            oldTraffic.endTime = System.currentTimeMillis()
            traffics.add(oldTraffic)
        }else {
            val traffic = Traffic(uid, (rxBytes - oldTraffic.rxBytes.bytes), (txBytes - oldTraffic.txBytes.bytes))

            traffic.startTime = oldTraffic.endTime
            traffic.endTime = System.currentTimeMillis()

            if (networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G){
                traffic.network = Networks.NETWORK_4G
            }else {
                traffic.network = Networks.NETWORK_3G
            }

            //TODO Falta agregar (al modelo Traffic o a uno que herede de el y entonces reemplace en esta clase a Traffic) la simID que pertenece el traffico
            //traffic.simId = simManager.getDefaultDataSim().id

            trafficRepository.create(traffic)
        }
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