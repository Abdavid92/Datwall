package com.smartsolutions.paquetes.ui.update

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.managers.SampleActivationManager
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(
    application: Application,
    private val updateManager: IUpdateManager,
    private val activationManager: IActivationManager
) : AndroidViewModel(application) {

    var androidApp: AndroidApp? = null
    private var currentDownload: Long? = null

    private var job: Job? = null


    fun getAndroidApp(): Boolean {
        return runBlocking {
            activationManager.getLocalLicense()?.let {
                androidApp = it.androidApp
                return@runBlocking true
            }
            return@runBlocking false
        }
    }


    fun getDownloadInCourse(listener: IUpdateManager.DownloadStatus): Boolean {
        updateManager.getSavedDownloadId()?.let {
            job?.cancel()
            job = null
            job = updateManager.getStatusDownload(it, listener)
            currentDownload = it
            return true
        }
        return false
    }


    fun downloadUpdate(mode: DownloadMode, listener: IUpdateManager.DownloadStatus): Boolean {
        androidApp?.let { app ->
            val url = updateManager.buildDynamicUrl(
                when (mode) {
                    DownloadMode.APKLIS_SERVER -> updateManager.BASE_URL_APKLIS
                    DownloadMode.DATWALL_SERVER -> updateManager.BASE_URL_HOSTINGER
                },
                app
            )

            currentDownload = updateManager.downloadUpdate(Uri.parse(url))

            updateManager.saveIdDownload(currentDownload)

            job?.cancel()
            job = null

            job = updateManager.getStatusDownload(currentDownload!!, listener)

            return true
        }

        return false
    }

    fun goStore() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(BuildConfig.APKLIS_URL)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(intent)
    }

    fun cancelDownload() {
        currentDownload?.let {
            updateManager.cancelDownload(it)
        }
    }

    fun installDownload(): Boolean {
        currentDownload?.let {
            if (updateManager.isInstallPermissionGranted()){
                if (!updateManager.installDownloadedPackage(it)){
                    updateManager.cancelDownload(it)
                    currentDownload = null
                    updateManager.saveIdDownload(null)
                }else {
                    return true
                }
            }
        }
        return false
    }

    fun stopJob() {
        job?.cancel()
        job = null
    }

    fun permissionInstallGranted(): Boolean {
        return updateManager.isInstallPermissionGranted()
    }


    enum class DownloadMode {
        APKLIS_SERVER,
        DATWALL_SERVER
    }

}