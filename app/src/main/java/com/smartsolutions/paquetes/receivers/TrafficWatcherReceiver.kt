package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.Observer
import com.smartsolutions.paquetes.repositories.TrafficRepository
import com.smartsolutions.paquetes.repositories.models.App
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Receiver que se lanza cada un segundo para recojer el tráfico de datos.
 * Se usa en apis 21 y 22 porque estas no tienen el servicio de estadísticas
 * de redes.
 * */
class TrafficWatcherReceiver @Inject constructor(
    private val appRepository: IAppRepository,
    private val trafficRepository: TrafficRepository
)
    : BroadcastReceiver(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    val traffics : MutableList<Traffic> = mutableListOf()

    init {
        appRepository.registerObserver(object : Observer() {
            override fun onDelete(apps: List<App>) {
                apps.forEach { app ->
                    traffics.firstOrNull { it.uid == app.uid }?.let {
                        traffics.remove(it)
                    }
                }
            }
        })
    }

    override fun onReceive(context: Context, intent: Intent) {
        launch {

            context.dataStore.data.map { preferences ->

                val dataConnected = preferences[PreferencesKeys.DATA_MOBILE_ON] == true

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

                        if (dataConnected && (traffic._rxBytes > 0 || traffic._txBytes > 0)) {
                            trafficRepository.create(traffic)
                        }

                        trafficOld._rxBytes = rxBytes
                        trafficOld._txBytes = txBytes
                        trafficOld.endTime = time
                    }
                }
            }
        }
    }
}