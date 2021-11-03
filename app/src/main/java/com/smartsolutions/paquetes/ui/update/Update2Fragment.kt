package com.smartsolutions.paquetes.ui.update

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentUpdate2Binding
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.NetworkUtils
import com.smartsolutions.paquetes.managers.contracts.IUpdateManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class Update2Fragment : BottomSheetDialogFragment(), IUpdateManager.DownloadStatus {

    private val permissionToInstall =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (!viewModel.installDownload()){
                Toast.makeText(requireContext(), getString(R.string.cant_not_install), Toast.LENGTH_SHORT).show()
                setDownloadingOptionsViews()
            }
        }

    private var _binding: FragmentUpdate2Binding? = null
    private val binding get() = _binding!!

    private val viewModel by viewModels<UpdateViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdate2Binding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!viewModel.getAndroidApp()) {
            dismiss()
        }

        if (viewModel.getDownloadInCourse(this)) {
            setDownloadProgressViews()
        } else {
            setDownloadingOptionsViews()
        }
    }


    private fun setDownloadingOptionsViews() {
        viewModel.androidApp?.let { androidApp ->
            binding.apply {
                constraintDownloadOptions.visibility = View.VISIBLE
                constraintDownloadProgress.visibility = View.GONE
                textNewVersion.text = "La nueva versión es la: ${androidApp.versionName}"
                textComments.text = androidApp.updateComments
                btnDirect.setOnClickListener {
                    viewModel.downloadUpdate(
                        UpdateViewModel.DownloadMode.APKLIS_SERVER,
                        this@Update2Fragment
                    )
                    setDownloadProgressViews()
                }
                btnStore.setOnClickListener {
                    viewModel.goStore()
                }
                btnDirectServer.setOnClickListener {
                    viewModel.downloadUpdate(
                        UpdateViewModel.DownloadMode.DATWALL_SERVER,
                        this@Update2Fragment
                    )
                    setDownloadProgressViews()
                }
            }
        }
    }

    private fun setDownloadProgressViews() {
        viewModel.androidApp?.let { androidApp ->
            binding.apply {
                constraintDownloadOptions.visibility = View.GONE
                constraintDownloadProgress.visibility = View.VISIBLE
                textVersionProgress.text = "Nueva versión: ${androidApp.versionName}"
                onPending()
            }
        }
    }


    override fun onRunning(downloaded: Long, total: Long) {
        binding.apply {
            val percent =
                DateCalendarUtils.calculatePercent(total.toDouble(), downloaded.toDouble())
            val dw = DataUnitBytes(downloaded).getValue()
            val tt = DataUnitBytes(total).getValue()

            imageStatusDownload.setImageResource(R.drawable.ic_download)

            textStatusDownload.text = getString(R.string.status_running)
            textStatusDownloadSummary.text = getString(R.string.status_running_summary)

            textPercentProgres.text = "$percent%"
            textRestProgres.text = "${dw.value} ${dw.dataUnit}/${tt.value} ${tt.dataUnit}"

            progressBar.isIndeterminate = false
            progressBar.max = total.toInt()
            progressBar.progress = downloaded.toInt()

            buttonActionDownload.text = getString(R.string.btn_cancel_download)
            buttonActionDownload.setOnClickListener {
                viewModel.cancelDownload()
            }
        }
    }

    override fun onFailed(reason: String) {
        binding.apply {

            imageStatusDownload.setImageResource(R.drawable.ic_error)

            textStatusDownload.text = getString(R.string.status_failed)
            textStatusDownloadSummary.text = reason

            textPercentProgres.text = ""
            textRestProgres.text = ""

            progressBar.isIndeterminate = false
            progressBar.max = 100
            progressBar.progress = 0

            buttonActionDownload.text = getString(R.string.btn_retry)
            buttonActionDownload.setOnClickListener {
                setDownloadingOptionsViews()
            }
        }
    }

    override fun onPending() {
        binding.apply {

            imageStatusDownload.setImageResource(R.drawable.ic_time_24)

            textStatusDownload.text = getString(R.string.status_pending)
            textStatusDownloadSummary.text = getString(R.string.status_pending_summary)

            textPercentProgres.text = ""
            textRestProgres.text = ""

            progressBar.isIndeterminate = true

            buttonActionDownload.text = getString(R.string.btn_cancel_download)
            buttonActionDownload.setOnClickListener {
                viewModel.cancelDownload()
            }
        }
    }

    override fun onPaused(reason: String) {
        binding.apply {

            imageStatusDownload.setImageResource(R.drawable.ic_pause_circle_outline_24)

            textStatusDownload.text = getString(R.string.status_paused)
            textStatusDownloadSummary.text = reason

            buttonActionDownload.text = getString(R.string.btn_cancel_download)
            buttonActionDownload.setOnClickListener {
                viewModel.cancelDownload()
            }
        }
    }

    override fun onSuccessful() {
        binding.apply {

            imageStatusDownload.setImageResource(R.drawable.ic_done)

            textStatusDownload.text = getString(R.string.status_done)
            textStatusDownloadSummary.text = getString(R.string.status_done_summary)

            textPercentProgres.text = ""
            textRestProgres.text = ""

            progressBar.isIndeterminate = false
            progressBar.progress = 100
            progressBar.max = 100

            buttonActionDownload.text = getString(R.string.btn_install)
            buttonActionDownload.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if(viewModel.permissionInstallGranted()){
                        if (!viewModel.installDownload()){
                            Toast.makeText(requireContext(), getString(R.string.cant_not_install), Toast.LENGTH_SHORT).show()
                            setDownloadingOptionsViews()
                        }
                    }else {
                        permissionToInstall.launch(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = Uri.parse("package:${requireContext().packageName}")
                            }
                        )
                    }
                }else {
                    if (!viewModel.installDownload()){
                        Toast.makeText(requireContext(), getString(R.string.cant_not_install), Toast.LENGTH_SHORT).show()
                        setDownloadingOptionsViews()
                    }
                }
            }
        }
    }

    override fun onCanceled() {
        binding.apply {

            imageStatusDownload.setImageResource(R.drawable.ic_cancel_24)

            textStatusDownload.text = getString(R.string.status_canceled)
            textStatusDownloadSummary.text = getString(R.string.status_canceled_summary)

            textPercentProgres.text = ""
            textRestProgres.text = ""

            progressBar.isIndeterminate = false
            progressBar.progress = 0
            progressBar.max = 100

            buttonActionDownload.text = getString(R.string.btn_retry)
            buttonActionDownload.setOnClickListener {
                setDownloadingOptionsViews()
            }
        }
    }


    companion object {
        fun newInstance(): Update2Fragment = Update2Fragment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        viewModel.stopJob()
        _binding = null
    }
}