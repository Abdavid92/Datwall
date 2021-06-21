package com.smartsolutions.paquetes

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.work.*
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class TrafficRegistration @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficRepository: ITrafficRepository,
    private val networkUtil: NetworkUtil,
    private val simManager: ISimManager,
    private val appRepository: IAppRepository,
    private val userDataBytesManager: IUserDataBytesManager
) : CoroutineScope {

    private var appsList = emptyList<App>()
    private val job = Job()

    fun startRegistration(){
        if (TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED.toLong()) {
            return
        }

        launch {
            appRepository.flow().collect {
                appsList = it
            }
        }

        val request = PeriodicWorkRequestBuilder<TrafficRegistrationWorker>(1000, TimeUnit.MILLISECONDS)
            .addTag(TRAFFIC_REGISTRATION_TAG)

        WorkManager.getInstance(context)
            .enqueue(request.build())
    }

    fun stopRegistration(){
        WorkManager.getInstance(context).cancelAllWorkByTag(TRAFFIC_REGISTRATION_TAG)
        traffics.clear()
        job.cancel()
    }


    suspend fun takeLollipopTraffic(){
        val simId = simManager.getDefaultDataSim().id
        val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G

        val trafficsToAdd = mutableListOf<Traffic>()

        appsList.forEach { app ->
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

            LocalBroadcastManager.getInstance(context)
                .sendBroadcast(Intent(ACTION_TRAFFIC).apply {
                    putExtra(EXTRA_RX_BAND_WITH, rx - rxBytes)
                    putExtra(EXTRA_TX_BAND_WITH, tx - txBytes)
                })

            userDataBytesManager.registerTraffic(
                rx - rxBytes,
                tx - txBytes,
                /*TODO:Temp*/0,
                isLte)

            rxBytes = rx
            txBytes = tx
        }
    }

    inner class TrafficRegistrationWorker(context: Context, workerParameters: WorkerParameters): Worker(context, workerParameters) {

        override fun doWork(): Result {
            return runBlocking {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                    takeLollipopTraffic()

                registerTraffic()

                return@runBlocking Result.success()
            }
        }

    }

    companion object {
        const val TRAFFIC_REGISTRATION_TAG = "traffic_registration_tag"
        const val GENERAL_TRAFFIC_UID = Int.MIN_VALUE

        private var traffics = mutableListOf<Traffic>()

        private var rxBytes = -1L
        private var txBytes = -1L

        const val ACTION_TRAFFIC = "com.smartsolutions.datwall.action.TRAFFIC"

        const val EXTRA_RX_BAND_WITH = "com.smartsolutions.datwall.extra.RX_BAND_WITH"

        const val EXTRA_TX_BAND_WITH = "com.smartsolutions.datwall.extra.TX_BAND_WITH"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

}