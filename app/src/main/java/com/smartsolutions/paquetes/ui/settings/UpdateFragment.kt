package com.smartsolutions.paquetes.ui.settings

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.UpdateManager
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import javax.inject.Inject

@AndroidEntryPoint
class UpdateFragment : Fragment(R.layout.fragment_update), IUpdateManager.DownloadStatus {

    @Inject
    lateinit var updateManager: IUpdateManager

    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var reason: TextView
    private var job: Job? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        status = view.findViewById(R.id.text_status_download)
        progress = view.findViewById(R.id.progressBar)
        reason = view.findViewById(R.id.status_reason)

        progress.isIndeterminate = true

        val url = Uri.parse("https://archive.apklis.cu/application/apk/Jefferson.covid19.world.data-v4.apk")

        val uriFile = updateManager.foundIfDownloaded(url)

        if (uriFile == null) {
            updateManager.downloadUpdate(url)?.let {
                job = updateManager.getStatusDownload(it, this)
            }
        }else {
            onSuccessful()
            updateManager.installDownloadedPackage(uriFile)
        }

    }

    override fun onRunning(downloaded: Long, total: Long) {
        progress.isIndeterminate = false
        progress.max = total.toInt()
        progress.progress = downloaded.toInt()
        status.text = "Descargando"
        reason.visibility = View.GONE
    }

    override fun onFailed(reason: String) {
        progress.isIndeterminate = false
        progress.max = 0
        progress.progress = 0
        status.text = "La descarga falló"
        this.reason.visibility = View.VISIBLE
        this.reason.text = reason
    }

    override fun onPending() {
        reason.visibility = View.GONE
        status.text = "La descarga está en espera"
        progress.isIndeterminate = true
    }

    override fun onPaused(reason: String) {
        status.text = "La descarga está en pausa"
        this.reason.visibility = View.VISIBLE
        this.reason.text = reason
    }

    override fun onSuccessful() {
        reason.visibility = View.GONE
        progress.isIndeterminate = false
        progress.max = 100
        progress.progress = 100
        status.text = "La descarga se completo correctamente"
    }

    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}