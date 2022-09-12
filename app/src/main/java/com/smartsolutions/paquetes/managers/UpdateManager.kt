package com.smartsolutions.paquetes.managers

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.managers.models.Update
import com.smartsolutions.paquetes.workers.UpdateApplicationStatusWorker
import cu.uci.apklisupdate.ApklisUpdate
import cu.uci.apklisupdate.UpdateCallback
import cu.uci.apklisupdate.model.AppUpdateInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


private const val updateApplicationStatusWorkerTag = "update_application_status_worker_tag"

class UpdateManager @Inject constructor(
    @ApplicationContext
    val context: Context
) : IUpdateManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    override suspend fun findUpdate(): Update? {
        return suspendCoroutine {

            ApklisUpdate.hasAppUpdate(context, object : UpdateCallback {

                override fun onError(e: Throwable) {
                    it.resume(null)
                }

                override fun onNewUpdate(appUpdateInfo: AppUpdateInfo) {

                    if (! appUpdateInfo.deleted && appUpdateInfo.public) {

                        it.resume(
                            Update(
                                appUpdateInfo.name,
                                appUpdateInfo.last_release.version_name,
                                Uri.parse("http://apklis.cu/application/${context.packageName}")
                            )
                        )
                    }
                }

                override fun onOldUpdate(appUpdateInfo: AppUpdateInfo) {
                    it.resume(null)
                }
            })
        }
    }


    override fun scheduleUpdateApplicationStatusWorker(intervalInHours: Long) {

        val request = PeriodicWorkRequestBuilder<UpdateApplicationStatusWorker>(
            intervalInHours,
            TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).addTag(updateApplicationStatusWorkerTag)
            .build()

        val workManager = WorkManager.getInstance(context)

        workManager.cancelAllWorkByTag(updateApplicationStatusWorkerTag)
        workManager.enqueue(request)
    }

    override fun cancelUpdateApplicationStatusWorker() {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(updateApplicationStatusWorkerTag)
    }

    override fun wasScheduleUpdateApplicationStatusWorker(): Boolean {
        val workManager = WorkManager.getInstance(context)

        return workManager.getWorkInfosByTag(updateApplicationStatusWorkerTag)
            .get().size > 0
    }
}