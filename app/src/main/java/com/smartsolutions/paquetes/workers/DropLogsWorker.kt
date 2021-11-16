package com.smartsolutions.paquetes.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.abdavid92.persistentlog.LogManager

class DropLogsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val manager = LogManager.newInstance()

        if (manager.all().size > 10000)
            manager.clear()

        return Result.success()
    }
}