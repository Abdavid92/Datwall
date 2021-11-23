package com.smartsolutions.paquetes.workers

import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import dagger.assisted.Assisted
import org.apache.commons.lang.time.DateUtils
import java.util.concurrent.TimeUnit

@HiltWorker
class TrafficDbOptimizerLollipop(
    @Assisted
    context: Context,
    @Assisted
    parameters: WorkerParameters,
    private val trafficRepository: ITrafficRepository,
    private val simManager: ISimManager
): CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {

        val traffics = mutableListOf<Traffic>()

        simManager.getInstalledSims().forEach { sim ->
            var traffic: Traffic? = null
            var hour = DateCalendarUtils.getStartAndFinishHour(System.currentTimeMillis())

            val oldTraffics = trafficRepository.getAll(sim.id).onEach { trafficDb ->
                when {
                    traffic == null -> {
                        traffic = trafficDb
                        hour = DateCalendarUtils.getStartAndFinishHour(trafficDb.startTime)
                    }
                    trafficDb.startTime in hour.first..hour.second -> {
                        traffic!! += trafficDb
                    }
                    else -> {
                        traffics.add(traffic!!)
                        traffic = trafficDb
                        hour = DateCalendarUtils.getStartAndFinishHour(trafficDb.startTime)
                    }
                }
            }

            if (oldTraffics.isNotEmpty() && traffics.isNotEmpty()) {
                trafficRepository.delete(oldTraffics)
                trafficRepository.create(traffics)
                traffics.clear()
            }
        }

        return Result.success()
    }


    companion object {

        private const val TAG_OPTIMIZER_WORKER = "tag_optimizer_worker"

        fun registerWorkerIfNeeded(context: Context, hoursInterval: Long){
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                val request = PeriodicWorkRequestBuilder<TrafficDbOptimizerLollipop>(
                    hoursInterval,
                    TimeUnit.HOURS
                )
                    .addTag(TAG_OPTIMIZER_WORKER)
                    .build()

                val workManager = WorkManager.getInstance(context)
                workManager.cancelAllWorkByTag(TAG_OPTIMIZER_WORKER)
                workManager.enqueue(request)
            }
        }

    }
}