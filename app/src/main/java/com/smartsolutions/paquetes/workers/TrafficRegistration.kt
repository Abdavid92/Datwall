package com.smartsolutions.paquetes.workers

import android.content.Context
import android.net.TrafficStats
import android.os.Build
import androidx.work.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TrafficRegistration @Inject constructor(
    @ApplicationContext
    private val context: Context,
) {

    fun startRegistration() {
        if (TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED.toLong()) {
            return
        }

        val request = PeriodicWorkRequestBuilder<TrafficRegistrationWorker>(1000, TimeUnit.MILLISECONDS)
            .addTag(TRAFFIC_REGISTRATION_TAG)

        WorkManager.getInstance(context)
            .enqueue(request.build())
    }

    fun stopRegistration() {
        WorkManager.getInstance(context).cancelAllWorkByTag(TRAFFIC_REGISTRATION_TAG)
        traffics.clear()
    }

    class TrafficRegistrationWorker(
        context: Context,
        workerParameters: WorkerParameters
    ) : Worker(context, workerParameters) {

        private val networkUtil: NetworkUtil
        private val userDataBytesManager: IUserDataBytesManager
        private val simManager: ISimManager
        private val trafficRepository: ITrafficRepository
        private val appRepository: IAppRepository

        init {
            val entryPoint = EntryPointAccessors
                .fromApplication(context, TrafficRegistrationEntryPoint::class.java)

            networkUtil = entryPoint.getNetworkUtil()
            userDataBytesManager = entryPoint.getUserDataBytesManager()
            simManager = entryPoint.getSimManager()
            trafficRepository = entryPoint.getTrafficRepository()
            appRepository = entryPoint.getAppRepository()
        }

        override fun doWork(): Result {
            return runBlocking {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    takeLollipopTraffic()

                registerTraffic()

                return@runBlocking Result.success()
            }
        }

        private suspend fun takeLollipopTraffic(){
            val simId = simManager.getDefaultDataSim().id
            val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G

            val trafficsToAdd = mutableListOf<Traffic>()

            appRepository.all().forEach { app ->
                processLollipopTraffic(
                    app.uid,
                    simId,
                    TrafficStats.getUidRxBytes(app.uid),
                    TrafficStats.getUidTxBytes(app.uid),
                    isLte)?.let { traffic ->
                    trafficsToAdd.add(traffic)
                }
            }

            processLollipopTraffic(
                GENERAL_TRAFFIC_UID,
                simId,
                TrafficStats.getMobileRxBytes(),
                TrafficStats.getMobileTxBytes(),
                isLte)?.let { traffic ->
                trafficsToAdd.add(traffic)
            }

            trafficRepository.create(trafficsToAdd)
        }

        private fun processLollipopTraffic(uid: Int, simID: String, rxBytes: Long, txBytes: Long, isLte: Boolean): Traffic? {
            var oldTraffic = traffics.firstOrNull{ it.uid == uid && it.simId == simID }

            if (oldTraffic == null){
                oldTraffic = Traffic(uid, rxBytes, txBytes, simID)
                oldTraffic.endTime = System.currentTimeMillis()
                traffics.add(oldTraffic)
            }else {
                val traffic = Traffic(uid, (rxBytes - oldTraffic.rxBytes.bytes), (txBytes - oldTraffic.txBytes.bytes), simID)

                traffic.startTime = oldTraffic.endTime
                traffic.endTime = System.currentTimeMillis()

                traffic.network = if (isLte){
                    Networks.NETWORK_4G
                }else {
                    Networks.NETWORK_3G
                }

                traffics[traffics.indexOf(oldTraffic)] = Traffic(uid, rxBytes, txBytes, simID)
                    .apply {
                        endTime = System.currentTimeMillis()
                    }

                return traffic
            }

            return null
        }

        private suspend fun registerTraffic() {
            if (rxBytes == -1L || txBytes == -1L) {
                rxBytes = TrafficStats.getMobileRxBytes()
                txBytes = TrafficStats.getMobileTxBytes()
            } else {
                val rx = TrafficStats.getMobileRxBytes()
                val tx = TrafficStats.getMobileTxBytes()
                val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G

                userDataBytesManager.registerTraffic(
                    rx - rxBytes,
                    tx - txBytes,
                    /*TODO:Temp*/0,
                    isLte)

                rxBytes = rx
                txBytes = tx
            }
        }

        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface TrafficRegistrationEntryPoint {
            fun getNetworkUtil(): NetworkUtil
            fun getUserDataBytesManager(): IUserDataBytesManager
            fun getSimManager(): ISimManager
            fun getTrafficRepository(): ITrafficRepository
            fun getAppRepository(): IAppRepository
        }
    }

    companion object {
        const val TRAFFIC_REGISTRATION_TAG = "traffic_registration_tag"
        const val GENERAL_TRAFFIC_UID = Int.MIN_VALUE

        private var traffics = mutableListOf<Traffic>()

        private var rxBytes = -1L
        private var txBytes = -1L
    }
}