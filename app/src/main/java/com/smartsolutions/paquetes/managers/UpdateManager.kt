package com.smartsolutions.paquetes.managers

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.workers.UpdateApplicationStatusWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val updateApplicationStatusWorkerTag = "update_application_status_worker_tag"

class UpdateManager @Inject constructor(
    @ApplicationContext
    val context: Context,
    val activationManager: IActivationManager
) : IUpdateManager {

    override suspend fun findUpdate(): AndroidApp? {
        activationManager.getDeviceApp()
            .getOrNull()?.let {
                if (it.androidApp.version > BuildConfig.VERSION_CODE) {
                    return it.androidApp
                }
            }
        return null
    }

    override fun scheduleFindUpdate(intervalInHours: Long) {

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

    override fun downloadUpdate(url: Uri): Long {

        val request = DownloadManager.Request(url)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setTitle(context.getString(R.string.update_notification_title))
            .setDestinationInExternalFilesDir(
                context,
                "Updates",
                url.lastPathSegment
            )

        val downloadManager = ContextCompat
            .getSystemService(context, DownloadManager::class.java) ?: throw NullPointerException()

        return downloadManager.enqueue(request)
    }

    override fun buildDynamicUrl(baseUrl: String, version: Int): String {
        val url = if (baseUrl.endsWith('/'))
            baseUrl
        else
            "$baseUrl/"

        return "$url${context.packageName}/v-$version.apk"
    }
}