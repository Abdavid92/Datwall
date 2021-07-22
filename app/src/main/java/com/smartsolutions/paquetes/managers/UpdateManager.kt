package com.smartsolutions.paquetes.managers

import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.workers.UpdateApplicationStatusWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.Exception


private const val updateApplicationStatusWorkerTag = "update_application_status_worker_tag"
private const val TYPE_DIR_UPDATES = "Updates"
private const val FILE_PROVIDER_AUTHORITY = "com.smartsolutions.paquetes.provider"

class UpdateManager @Inject constructor(
    @ApplicationContext
    val context: Context,
    val activationManager: IActivationManager
) : IUpdateManager {

    private val downloadManager = ContextCompat
        .getSystemService(context, DownloadManager::class.java)
        ?: throw NullPointerException()


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
            .setDescription(url.lastPathSegment)
            .setDestinationInExternalFilesDir(
                context,
                TYPE_DIR_UPDATES,
                url.lastPathSegment
            )

        val id = downloadManager.enqueue(request)

       saveIdDownload(id)

        return id
    }


    override fun getStatusDownload(id: Long, callback: IUpdateManager.DownloadStatus): Job {
        return GlobalScope.launch(Dispatchers.IO) {

            var isFinished = false

            var totalBytes = -1L

            while (!isFinished) {
                val cursor: Cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(id))

                if (cursor.moveToFirst()) {

                    when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
                        DownloadManager.STATUS_FAILED -> {
                            isFinished = true
                            withContext(Dispatchers.Main) {
                                callback.onFailed(getReason(cursor))
                            }
                        }
                        DownloadManager.STATUS_PAUSED -> {
                            withContext(Dispatchers.Main) {
                                callback.onPaused(getReason(cursor))
                            }
                        }
                        DownloadManager.STATUS_PENDING -> {
                            withContext(Dispatchers.Main) {
                                callback.onPending()
                            }
                        }
                        DownloadManager.STATUS_RUNNING -> {
                            if (totalBytes == -1L) {
                                totalBytes =
                                    cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            }
                            if (totalBytes >= 0) {
                                withContext(Dispatchers.Main) {
                                    callback.onRunning(
                                        cursor.getLong(
                                            cursor.getColumnIndex(
                                                DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR
                                            )
                                        ), totalBytes
                                    )
                                }
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            isFinished = true
                            withContext(Dispatchers.Main) {
                                callback.onSuccessful()
                            }
                        }
                    }
                } else {
                    isFinished = true
                    withContext(Dispatchers.Main){
                        callback.onCanceled()
                    }
                }

                delay(200)
            }
        }
    }


    private fun getReason(cursor: Cursor): String {
        return when (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_REASON))) {
            DownloadManager.ERROR_CANNOT_RESUME -> "No se pudo reanudar la descarga"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "No se encuentra la tarjeta SD"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "Ya la actualización fue descargada"
            DownloadManager.ERROR_HTTP_DATA_ERROR,
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Error de conexión HTTP"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "No hay suficiente espacio para descargar"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Demasiadas redirecciones"
            DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "Esperando una red Wi-Fi"
            DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "Esperando conexión a Internet"
            DownloadManager.PAUSED_WAITING_TO_RETRY -> "Error de red. Esperando para reintentar"
            else -> "Razón Desconocida"
        }
    }


    override fun installDownloadedPackage(idDownload: Long): Boolean {
        downloadManager.getUriForDownloadedFile(idDownload)?.let { uri ->
            return installDownloadedPackage(uri)
        }
        return false
    }


    override fun installDownloadedPackage(uri: Uri): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.applicationContext.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }


    override fun foundIfDownloaded(url: Uri): Uri? {
        val file = File(context.getExternalFilesDir(TYPE_DIR_UPDATES), url.lastPathSegment ?: " ")

        if (file.exists()){
            try {
                return FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
            }catch (e: Exception){
            }
        }
        return null
    }

    override fun isDownloaded(id: Long): Boolean {
        val cursor: Cursor =
            downloadManager.query(DownloadManager.Query().setFilterById(id))

        if (cursor.moveToFirst()) {
            return cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }

        return false
    }


    override fun buildDynamicUrl(baseUrl: String, version: Int): String {
        val url = if (baseUrl.endsWith('/'))
            baseUrl
        else
            "$baseUrl/"

        return "$url${context.packageName}-v$version.apk"
    }


    override fun buildDynamicUrl(baseUrl: String, packageName: String,  version: Int): String {
        val url = if (baseUrl.endsWith('/'))
            baseUrl
        else
            "$baseUrl/"

        return "$url${packageName}-v$version.apk"
    }

    override fun buildDynamicUrl(baseUrl: String, androidApp: AndroidApp): String {
        return buildDynamicUrl(baseUrl, androidApp.packageName, androidApp.version)
    }


    override fun isInstallPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return context.packageManager.canRequestPackageInstalls()
        }

        return true
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun requestInstallPermission(requestCode: Int, activity: Activity) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            setData(Uri.parse("package:${context.packageName}"))
        }
        activity.startActivityForResult(intent, requestCode)
    }


    override fun getSavedDownloadId(): Long? {
        var id: Long?
        runBlocking {
            id = context.dataStore.data.firstOrNull()?.get(PreferencesKeys.DOWNLOAD_UPDATE_ID)
        }
        if (id == -1L){
            id = null
        }
        return id
    }


    override fun saveIdDownload(id: Long?) {
        runBlocking {
            context.dataStore.edit {
                it[PreferencesKeys.DOWNLOAD_UPDATE_ID] = id ?: -1
            }
        }
    }

    override fun cancelDownload(id: Long) {
        DownloadManager.ACTION_NOTIFICATION_CLICKED
        downloadManager.remove(id)
        saveIdDownload(null)
    }


}