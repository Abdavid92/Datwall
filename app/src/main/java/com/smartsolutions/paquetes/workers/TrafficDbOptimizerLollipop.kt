package com.smartsolutions.paquetes.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import dagger.assisted.Assisted
import org.apache.commons.lang.time.DateUtils

@HiltWorker
class TrafficDbOptimizerLollipop(
    @Assisted
    context: Context,
    @Assisted
    parameters: WorkerParameters,
    private val trafficRepository: ITrafficRepository,
    private val simManager: ISimManager,
    private val dateCalendarUtils: DateCalendarUtils
): CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {

        val traffics = mutableListOf<Traffic>()
        val dayPeriod = dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_TODAY)

        simManager.getInstalledSims().forEach { sim ->
            var traffic: Traffic? = null
            var periodFinish = 0L

            val oldTraffics = trafficRepository.getByTime(sim.id, dayPeriod.first, dayPeriod.second).onEach { trafficDb ->
                when {
                    traffic == null -> {
                        traffic = trafficDb
                        periodFinish = trafficDb.startTime + DateUtils.MILLIS_PER_HOUR
                    }
                    trafficDb.startTime <= periodFinish -> {
                        traffic!! += trafficDb
                    }
                    else -> {
                        traffics.add(traffic!!)
                        traffic = trafficDb
                        periodFinish = trafficDb.startTime + DateUtils.MILLIS_PER_HOUR
                    }
                }
            }

            trafficRepository.delete(oldTraffics)
            trafficRepository.create(traffics)
            traffics.clear()
        }

        return Result.success()
    }
}