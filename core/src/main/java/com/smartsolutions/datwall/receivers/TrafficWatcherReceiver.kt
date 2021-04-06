package com.smartsolutions.datwall.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import com.smartsolutions.datwall.managers.models.Traffic
import com.smartsolutions.datwall.repositories.IAppRepository
import com.smartsolutions.datwall.repositories.Observer
import com.smartsolutions.datwall.repositories.TrafficRepository
import com.smartsolutions.datwall.repositories.models.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext


class TrafficWatcherReceiver @Inject constructor(
    private val appRepository: IAppRepository,
    private val trafficRepository: TrafficRepository)
    : BroadcastReceiver(), CoroutineScope {

    init {
        appRepository.registerObserver(object : Observer(){
            override fun onDelete(apps: List<App>) {
                apps.forEach { app ->
                    traffics.firstOrNull { it.uid == app.uid }?.let {
                        traffics.remove(it)
                    }
                }
            }
        })
    }

    val traffics : MutableList<Traffic> = mutableListOf()

    override fun onReceive(context: Context?, intent: Intent?) {
        launch {
            appRepository.all.forEach { app->
                val rxBytes = TrafficStats.getUidRxBytes(app.uid)
                val txBytes = TrafficStats.getUidTxBytes(app.uid)

                var trafficOld = traffics.firstOrNull { it.uid == app.uid }
                val time = System.currentTimeMillis()

                if (trafficOld == null){
                    trafficOld = Traffic(app.uid, rxBytes, txBytes)
                    trafficOld.endTime = time
                    traffics.add(trafficOld)
                }else {
                    val traffic = Traffic(app.uid, rxBytes - trafficOld._rxBytes, txBytes - trafficOld._txBytes)
                    traffic.startTime = trafficOld.endTime
                    traffic.endTime = time
                    if (traffic._rxBytes > 0 || traffic._txBytes > 0) {
                        trafficRepository.create(traffic)
                    }
                    trafficOld._rxBytes = rxBytes
                    trafficOld._txBytes = txBytes
                    trafficOld.endTime = time
                    TODO("Falta por verificar si hay datos encedidos")
                }
            }
        }

    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO


}