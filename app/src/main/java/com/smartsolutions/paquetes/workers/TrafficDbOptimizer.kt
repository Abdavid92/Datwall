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
import com.smartsolutions.paquetes.repositories.SimRepository
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.contracts.ITrafficRepository
import com.smartsolutions.paquetes.repositories.contracts.IUsageGeneralRepository
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import dagger.assisted.Assisted
import java.util.*
import java.util.concurrent.TimeUnit

@HiltWorker
class TrafficDbOptimizer(
    @Assisted
    context: Context,
    @Assisted
    parameters: WorkerParameters,
    private val trafficRepository: ITrafficRepository,
    private val usageGeneralRepository: IUsageGeneralRepository,
    private val simRepository: ISimRepository,
    private val dateCalendarUtils: DateCalendarUtils
) : CoroutineWorker(context, parameters) {


    override suspend fun doWork(): Result {

        kotlin.runCatching {
            val limitTime = dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_YESTERDAY).first

            val oldLimit = dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_YEAR).first

            compactUsageGeneral(limitTime, oldLimit)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
                compactTrafficsLollipop(limitTime, oldLimit)
        }


        return Result.success()
    }

    private suspend fun compactUsageGeneral(limitTime: Long, oldLimit: Long) {
        val all = usageGeneralRepository.all()

        simRepository.all().forEach { sim ->
            val usages = mutableListOf<UsageGeneral>()
            val usagesToAdd = mutableListOf<UsageGeneral>()

            var period = 0L to 1L

            val old = all.filter { it.simId == sim.id }.sortedBy { it.date }.onEach { usageDb ->
                if (usageDb.date >= oldLimit) {
                    val usa = usagesToAdd.firstOrNull { it.type == usageDb.type }

                    when {
                        usa == null -> {
                            usagesToAdd.add(usageDb)
                            period = if (usageDb.date >= limitTime) {
                                DateCalendarUtils.getStartAndFinishHour(usageDb.date)
                            } else {
                                val day = Date(usageDb.date)
                                DateCalendarUtils.getZeroHour(
                                    day
                                ).time to DateCalendarUtils.getLastHour(
                                    day
                                ).time
                            }
                        }
                        usageDb.date in period.first..period.second -> {
                            usagesToAdd[usagesToAdd.indexOf(usa)] += usageDb
                        }
                        else -> {
                            usages.addAll(usagesToAdd)
                            usagesToAdd.clear()
                            usagesToAdd.add(usageDb)
                            period = if (usageDb.date >= limitTime) {
                                DateCalendarUtils.getStartAndFinishHour(usageDb.date)
                            } else {
                                val day = Date(usageDb.date)
                                DateCalendarUtils.getZeroHour(
                                    day
                                ).time to DateCalendarUtils.getLastHour(
                                    day
                                ).time
                            }
                        }
                    }
                }
            }

            usages.addAll(usagesToAdd)

            if (old.isNotEmpty() && usages.isNotEmpty()) {
                usageGeneralRepository.delete(*old.toTypedArray())
                usageGeneralRepository.create(*usages.toTypedArray())
            }

        }
    }


    private suspend fun compactTrafficsLollipop(limitTime: Long, oldLimit: Long) {
        simRepository.all().forEach { sim ->
            val traffics = mutableListOf<Traffic>()
            val trafficsToAdd = mutableListOf<Traffic>()
            var period = 0L to 1L

            val oldTraffics = trafficRepository.getAll(sim.id).onEach { trafficDb ->
                if (trafficDb.startTime >= oldLimit) {
                    val traff = trafficsToAdd.firstOrNull { it.uid == trafficDb.uid }

                    when {
                        traff == null -> {
                            trafficsToAdd.add(trafficDb)
                            period = if (trafficDb.startTime >= limitTime) {
                                DateCalendarUtils.getStartAndFinishHour(trafficDb.startTime)
                            } else {
                                val day = Date(trafficDb.startTime)
                                DateCalendarUtils.getZeroHour(
                                    day
                                ).time to DateCalendarUtils.getLastHour(
                                    day
                                ).time
                            }
                        }
                        trafficDb.startTime in period.first..period.second -> {
                            trafficsToAdd[trafficsToAdd.indexOf(traff)] += trafficDb
                        }
                        else -> {
                            traffics.addAll(trafficsToAdd)
                            trafficsToAdd.clear()
                            trafficsToAdd.add(trafficDb)
                            period = if (trafficDb.startTime >= limitTime) {
                                DateCalendarUtils.getStartAndFinishHour(trafficDb.startTime)
                            } else {
                                val day = Date(trafficDb.startTime)
                                DateCalendarUtils.getZeroHour(
                                    day
                                ).time to DateCalendarUtils.getLastHour(
                                    day
                                ).time
                            }
                        }
                    }
                }
            }

            traffics.addAll(trafficsToAdd)

            if (oldTraffics.isNotEmpty() && traffics.isNotEmpty()) {
                trafficRepository.delete(oldTraffics)
                trafficRepository.create(traffics)
            }
        }
    }


    companion object {

        private const val TAG_OPTIMIZER_WORKER = "tag_optimizer_worker"

        fun registerWorkerIfNeeded(context: Context, hoursInterval: Long) {
            val request = PeriodicWorkRequestBuilder<TrafficDbOptimizer>(
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