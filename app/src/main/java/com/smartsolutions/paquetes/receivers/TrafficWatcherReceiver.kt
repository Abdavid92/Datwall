package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.Build
import com.smartsolutions.paquetes.DatwallApplication
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.TrafficRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receiver que se lanza cada un segundo para recojer el tráfico de datos.
 * */
class TrafficWatcherReceiver @Inject constructor(
    private val appRepository: IAppRepository,
    private val trafficRepository: TrafficRepository,
    private val userDataBytesManager: IUserDataBytesManager,
    private val networkUtil: NetworkUtil
): BroadcastReceiver() {

    private val traffics : MutableList<Traffic> = mutableListOf()

    init {
        globalRxBytes = TrafficStats.getMobileRxBytes()
        globalTxBytes = TrafficStats.getMobileTxBytes()

        GlobalScope.launch(Dispatchers.IO) {
            appRepository.flow().collect { apps ->
                val listToRemove = mutableListOf<Traffic>()

                traffics.forEach { traffic ->
                    if (apps.firstOrNull { it.uid == traffic.uid } == null)
                        listToRemove.add(traffic)
                }

                traffics.removeAll(listToRemove)
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch(Dispatchers.Default) {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1)
                registerTrafficByApp(context)

            registerTraffic()
        }
    }

    private suspend fun registerTraffic() {
        val rxBytes = TrafficStats.getMobileRxBytes() - globalRxBytes
        val txBytes = TrafficStats.getMobileTxBytes() - globalTxBytes
        globalRxBytes += rxBytes
        globalTxBytes += txBytes

        val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G

        userDataBytesManager.registerTraffic(rxBytes, txBytes, isLte)
    }

    private suspend fun registerTrafficByApp(context: Context) {
        val dataConnected = (context.applicationContext as DatwallApplication).dataMobileOn

        appRepository.all().forEach { app->
            val rxBytes = TrafficStats.getUidRxBytes(app.uid)
            val txBytes = TrafficStats.getUidTxBytes(app.uid)

            var trafficOld = traffics.firstOrNull { it.uid == app.uid }
            val time = System.currentTimeMillis()

            if (trafficOld == null) {
                trafficOld = Traffic(app.uid, rxBytes, txBytes)
                trafficOld.endTime = time
                traffics.add(trafficOld)
            } else {
                val traffic = Traffic(app.uid, rxBytes - trafficOld._rxBytes, txBytes - trafficOld._txBytes)
                traffic.startTime = trafficOld.endTime
                traffic.endTime = time

                if (dataConnected && (traffic._rxBytes > 0 || traffic._txBytes > 0)) {
                    trafficRepository.create(traffic)
                }

                trafficOld._rxBytes = rxBytes
                trafficOld._txBytes = txBytes
                trafficOld.endTime = time
            }
        }
    }

    companion object {

        private var globalRxBytes = 0L
        private var globalTxBytes = 0L

    }
}