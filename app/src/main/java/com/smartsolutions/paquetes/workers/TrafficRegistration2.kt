package com.smartsolutions.paquetes.workers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.smartsolutions.paquetes.watcher.Watcher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Programa un trabajo que se lanza cada un segundo para registrar el tráfico de red.
 * */
class TrafficRegistration2 @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val networkUtil: NetworkUtil,
    private val userDataBytesManager: IUserDataBytesManager,
    private val simManager: ISimManager,
    private val trafficRepository: ITrafficRepository,
    private val appRepository: IAppRepository
): CoroutineScope {

    private val tickBroadcastReceiver = TickBroadcastReceiver()

    /**
     * Inicia el registro del tráfico de red. Si el dispositivo
     * no es compatible con la clase [TrafficStats], el trabajo de registro
     * no se iniciará y por lo tanto no se registrará ningún trafico.
     * */
    fun startRegistration() {
        if (TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED.toLong()) {
            return
        }

        tickBroadcastReceiver.register(context)
    }

    /**
     * Detiene el registro de red.
     * */
    fun stopRegistration() {
        tickBroadcastReceiver.unregister(context)
        traffics.clear()
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

            LocalBroadcastManager.getInstance(context).sendBroadcast(
                Intent(ACTION_TRAFFIC_REGISTRATION)
                    .putExtra(EXTRA_TRAFFIC_TX, tx - txBytes)
                    .putExtra(EXTRA_TRAFFIC_RX, rx - rxBytes)
            )

            rxBytes = rx
            txBytes = tx
        }
    }


    inner class TickBroadcastReceiver: BroadcastReceiver(){

        private var isRegistered = false

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Watcher.ACTION_TICKTOCK) {
                launch {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                        takeLollipopTraffic()

                    registerTraffic()
                }
            }
        }


        fun register(context: Context) {
            if (!isRegistered) {
                LocalBroadcastManager.getInstance(context)
                    .registerReceiver(this, IntentFilter(Watcher.ACTION_TICKTOCK))
                isRegistered = true
            }
        }


        fun unregister(context: Context){
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this)
            isRegistered = false
        }

    }


    companion object {
        const val TRAFFIC_REGISTRATION_TAG = "traffic_registration_tag"
        const val GENERAL_TRAFFIC_UID = Int.MIN_VALUE

        private var traffics = mutableListOf<Traffic>()

        const val ACTION_TRAFFIC_REGISTRATION = "com.smartsolutions.paquetes.action.TRAFFIC_REGISTRATION"
        const val EXTRA_TRAFFIC_RX = "com.smartsolutions.paquetes.extra.TRAFFIC_RX"
        const val EXTRA_TRAFFIC_TX = "com.smartsolutions.paquetes.extra.TRAFFIC_TX"

        private var rxBytes = -1L
        private var txBytes = -1L
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
}