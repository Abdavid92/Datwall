package com.smartsolutions.paquetes.ui.activation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentApplicationStatusBinding
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.ui.IReplaceFragments
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import com.smartsolutions.paquetes.ui.update.Update2Fragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

@AndroidEntryPoint
class ApplicationStatusFragment : AbstractSettingsFragment(),
    IActivationManager.ApplicationStatusListener, CoroutineScope {

    private val request =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            whileWifiIsNotEnabled()
        }

    private val viewModel by viewModels<ApplicationStatusViewModel>()

    private var _binding: FragmentApplicationStatusBinding? = null

    private val binding get() = _binding!!

    private var isWaitingWifi: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentApplicationStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAction.setOnClickListener {
            Toast.makeText(
                requireContext(),
                getString(R.string.must_have_complete),
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.buttonStartVerify.setOnClickListener {
            binding.cardStartVerify.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE

            if (!viewModel.getApplicationStatus(this)) {
                if (Build.VERSION.SDK_INT == Build.VERSION_CODES.P) {
                    viewModel.requestEnableWifiPie()
                    viewModel.getApplicationStatus(this)
                } else {
                    setStatusResult(
                        getString(R.string.wifi_off_title),
                        getString(R.string.wifi_off_description),
                        R.drawable.ic_wifi_off_24,
                        getString(R.string.button_wifi_on)
                    ) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            requestWifi()
                        }
                    }
                }
            }
        }

    }


    override fun onPurchased(license: License) {
        setStatusResult(
            getString(R.string.is_purchased_title),
            getString(R.string.is_purchased_description),
            R.drawable.ic_done_outline_24
        )
        binding.btnAction.setOnClickListener {
            complete()
        }
    }

    override fun onDiscontinued(license: License) {
        binding.btnAction.visibility = View.GONE
        setStatusResult(
            getString(R.string.is_descontinued_title),
            getString(R.string.is_descontinued_description),
            R.drawable.ic_sentiment_very_dissatisfied_24,
            getString(R.string.btn_close)
        ) {
            activity?.finishAffinity()
        }

        binding.apply {
            btnCopyToClipboard.visibility = View.VISIBLE
            btnCopyToClipboard.setOnClickListener {
                viewModel.copyClipboardIdentifierDevice()
            }
        }
    }

    override fun onDeprecated(license: License) {
        binding.btnAction.visibility = View.GONE
        setStatusResult(
            getString(R.string.is_deprecated_title),
            getString(R.string.is_deprecated_description),
            R.drawable.ic_update_24,
            getString(R.string.btn_update)
        ) {
            Update2Fragment.newInstance()
                .show(childFragmentManager, null)
        }
    }

    override fun onTrialPeriod(license: License, isTrialPeriod: Boolean) {
        if (isTrialPeriod) {
            val days = license.androidApp.trialPeriod - license.daysInUse()
            setStatusResult(
                getString(R.string.is_trial_period_title),
                "Puede seguir usando la app en período de prueba por $days días",
                R.drawable.ic_timer_24
            )
            binding.btnPurchaseNown.apply {
                visibility = View.VISIBLE
                setOnClickListener {
                    if (activity is IReplaceFragments) {
                        (activity as IReplaceFragments)
                            .replace(Purchase2Fragment.newInstance())
                    }
                }
            }
            binding.btnAction.setOnClickListener {
                complete()
            }
        } else {
            binding.btnAction.visibility = View.GONE
            setStatusResult(
                getString(R.string.is_trial_period_expire_title),
                getString(R.string.is_trial_period_expire_description),
                R.drawable.ic_monetization_24,
                getString(R.string.purchase_licence)
            ) {

                if (activity is IReplaceFragments) {
                    (activity as IReplaceFragments)
                        .replace(Purchase2Fragment.newInstance())
                }
            }
        }
    }

    override fun onTooMuchOld(license: License) {
        //Empty Not Use
    }

    override fun onFailed(th: Throwable) {
        setStatusResult(
            getString(R.string.is_failed_title),
            "Se produjo un error que impidió revisar el estado de su licencia.\n" +
                    "Esta es la traza del error(${th.message})\n" +
                    "Si el error persiste comunique este error al equipo de desarrolladores",
            R.drawable.ic_error,
            getString(R.string.btn_retry)
        ) {
            binding.constraintStatusResult.visibility = View.GONE
            binding.progressBar.visibility = View.VISIBLE
            viewModel.getApplicationStatus(this)
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestWifi() {
        request.launch(
            Intent(Settings.Panel.ACTION_WIFI)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun whileWifiIsNotEnabled() {
        if (isWaitingWifi == null) {
            isWaitingWifi = launch {
                while (!viewModel.isWifiEnabled()) {
                    delay(1000)
                }
                if (viewModel.isWifiEnabled()) {
                    withContext(Dispatchers.Main) {
                        binding.constraintStatusResult.visibility = View.GONE
                        binding.progressBar.visibility = View.VISIBLE
                        viewModel.getApplicationStatus(this@ApplicationStatusFragment)
                    }
                }
            }
        }
    }

    private fun setStatusResult(
        title: String,
        description: String,
        @DrawableRes icon: Int,
        textButton: String,
        listener: View.OnClickListener
    ) {
        binding.apply {
            progressBar.visibility = View.GONE
            imageIcon.setImageResource(icon)
            titleStatusResult.text = title
            descriptionStatusResult.text = description
            buttonSatusResult.text = textButton
            buttonSatusResult.setOnClickListener(listener)
            constraintStatusResult.visibility = View.VISIBLE
        }
    }

    private fun setStatusResult(title: String, description: String, @DrawableRes icon: Int) {
        binding.apply {
            progressBar.visibility = View.GONE
            titleStatusResult.text = title
            imageIcon.setImageResource(icon)
            descriptionStatusResult.text = description
            buttonSatusResult.visibility = View.GONE
            constraintStatusResult.visibility = View.VISIBLE
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        isWaitingWifi?.cancel()
        isWaitingWifi = null
    }

    companion object {
        fun newInstance() = ApplicationStatusFragment()
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

}