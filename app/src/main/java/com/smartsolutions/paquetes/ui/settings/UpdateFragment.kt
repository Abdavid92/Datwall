package com.smartsolutions.paquetes.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import javax.inject.Inject

@AndroidEntryPoint
class UpdateFragment(
    private val androidApp: AndroidApp
): BottomSheetDialogFragment(), IUpdateManager.DownloadStatus {

    @Inject
    lateinit var updateManager: IUpdateManager

    private var job: Job? = null

    private var id: Long? = null

    private lateinit var linearDownload: LinearLayout
    private lateinit var linearStatus: LinearLayout
    private lateinit var textStatus: TextView
    private lateinit var textReason: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var buttonStop: Button
    private lateinit var buttonDownload: Button


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_update, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        initializeViews(view)

         id = updateManager.getSavedDownloadId()

        if (id != null){
            updateManager.getStatusDownload(id!!, this)
            linearDownload.visibility = View.GONE
            linearStatus.visibility = View.VISIBLE
        }else {
            findOrDownload()
        }
    }


    private fun initializeViews(view: View){
        val textVersion: TextView = view.findViewById(R.id.text_version)
        val textComments: TextView = view.findViewById(R.id.text_comments)
        val buttonStore: Button = view.findViewById(R.id.button_store)

        linearDownload = view.findViewById(R.id.lin_download)
        linearStatus = view.findViewById(R.id.lin_status_download)
        textStatus = view.findViewById(R.id.text_status)
        textReason = view.findViewById(R.id.text_reason)
        progressBar = view.findViewById(R.id.progressBar)
        buttonStop = view.findViewById(R.id.button_stop_download)
        buttonDownload = view.findViewById(R.id.button_download)

        buttonStop.setOnClickListener {
            id?.let {
                updateManager.cancelDownload(it)
            }
        }

        textComments.text = androidApp.updateComments
        textVersion.text = "La nueva versión es la: ${androidApp.versionName}"

        buttonStore.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.apklis.cu/application/com.smartsolutions.paquetes")
            startActivity(intent)
        }
    }


    /**
     * Busca el apk si ha sido descargado, sino lo descarga y luego lo instala
     */
    private fun findOrDownload(){
        linearStatus.visibility = View.GONE
        linearDownload.visibility = View.VISIBLE

        val url = Uri.parse(
            updateManager.buildDynamicUrl(
                updateManager.BASE_URL_APKLIS,
                androidApp.packageName,
                androidApp.version
            )
        )

        val uriFile = updateManager.foundIfDownloaded(url)

        if (uriFile == null) {
            buttonStop.text = "Cancelar Descarga"
            buttonStop.setOnClickListener {
                id?.let {
                    updateManager.cancelDownload(it)
                }
            }
            setStatusAndReason(
                "Comenzando Descarga",
                "Inicializando descarga"
            )
            buttonDownload.setOnClickListener {
                id = updateManager.downloadUpdate(url)
                job = updateManager.getStatusDownload(id!!, this)
                linearDownload.visibility = View.GONE
                linearStatus.visibility = View.VISIBLE
            }
        } else {
            buttonDownload.text = "Instalar Ahora"
            buttonDownload.setOnClickListener {
                updateManager.installDownloadedPackage(uriFile)
            }
        }
    }


    override fun onRunning(downloaded: Long, total: Long) {
       setProgressBar(downloaded.toInt(), total.toInt())
        val part = DataUnitBytes(downloaded).getValue()
        val all = DataUnitBytes(total).getValue()
        setStatusAndReason(
            "Descargando",
            "Descargado ${Math.round(part.value*100.0)/100.0} ${part.dataUnit.name} de ${Math.round(all.value*100.0)/100.0} ${all.dataUnit.name}"
        )
    }


    override fun onFailed(reason: String) {
        setProgressBar(100, 100)
        setStatusAndReason(
            "Descarga Fallida",
            reason
        )
        buttonStop.text = "Volver a Intentar"
        buttonStop.setOnClickListener {
            id?.let {
                updateManager.cancelDownload(it)
            }
            findOrDownload()
        }
    }


    override fun onPending() {
        setStatusAndReason(
            "Descarga Pendiente",
            "La descarga está en cola"
        )
    }


    override fun onPaused(reason: String) {
        setStatusAndReason(
            "Descarga Pausada",
            reason
        )
    }


    override fun onSuccessful() {
        setProgressBar(100, 100)
        updateManager.saveIdDownload(null)
        setStatusAndReason(
            "Descarga Completa",
            "La descarga se completó correctamente"
        )

        buttonStop.text = "Instalar Ahora"
        buttonStop.setOnClickListener {
            id?.let {
                updateManager.installDownloadedPackage(it)
            }
        }
    }


    override fun onCanceled() {
        setProgressBar(100, 100)
        setStatusAndReason(
            "Descarga Cancelada",
            "La descarga fue cancelada por usted"
        )
        buttonStop.text = "Volver a Intentar"
        buttonStop.setOnClickListener {
            id?.let {
                updateManager.saveIdDownload(null)
            }
            findOrDownload()
        }
    }


    private fun setStatusAndReason(status: String, reason: String){
        textStatus.text = status
        textReason.text = reason
    }


    private fun setProgressBar(progress: Int, total: Int){
        progressBar.isIndeterminate = false
        progressBar.max = total
        progressBar.progress = progress
    }


    override fun onDestroy() {
        job?.cancel()
        super.onDestroy()
    }
}