package com.smartsolutions.paquetes.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import javax.inject.Inject

@AndroidEntryPoint
class UpdateFragment(
    private val androidApp: AndroidApp
    ) : BottomSheetDialogFragment(), IUpdateManager.DownloadStatus {

    @Inject
    lateinit var updateManager: IUpdateManager


    private var job: Job? = null


    private lateinit var text_version: TextView
    private lateinit var text_comments: TextView
    private lateinit var button_download: Button
    private lateinit var button_store: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        text_version = view.findViewById(R.id.text_version)
        text_comments = view.findViewById(R.id.text_comments)
        button_download = view.findViewById(R.id.button_download)
        button_store = view.findViewById(R.id.button_store)


        text_comments.text = androidApp.updateComments
        text_version.text = "La nueva versión es la: ${androidApp.versionName}"

        button_store.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.apklis.cu/application/com.smartsolutions.paquetes")
            startActivity(intent)
        }

        val url = Uri.parse(
            updateManager.buildDynamicUrl(
                updateManager.BASE_URL_APKLIS,
                androidApp.packageName,
                androidApp.version
            )
        )


        val uriFile = updateManager.foundIfDownloaded(url)

        if (uriFile == null) {
            button_download.setOnClickListener {
                val id = updateManager.downloadUpdate(url)
                job = updateManager.getStatusDownload(id, this)
            }
        } else {
            button_download.text = "Instalar Ahora"
            button_download.setOnClickListener {
                updateManager.installDownloadedPackage(uriFile)
            }
        }
    }

    override fun onRunning(downloaded: Long, total: Long) {

    }

    override fun onFailed(reason: String) {

    }

    override fun onPending() {

    }

    override fun onPaused(reason: String) {

    }

    override fun onSuccessful() {

    }

    override fun onCanceled() {

    }


}