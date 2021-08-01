package com.smartsolutions.paquetes.ui.activation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.databinding.FragmentApplicationStatusBinding
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import com.smartsolutions.paquetes.ui.settings.UpdateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusFragment :
    AbstractSettingsFragment(R.layout.fragment_application_status),
    IActivationManager.ApplicationStatusListener
{

    private val viewModel by viewModels<ApplicationStatusViewModel>()

    private lateinit var binding: FragmentApplicationStatusBinding

    override fun isRequired(): Boolean {
        return true
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentApplicationStatusBinding.inflate(
            inflater,
            container,
            false
        )

        binding.waiting = false

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAction.setOnClickListener {
            viewModel.getApplicationStatus(this)
            binding.waiting = true
        }
        binding.btnLater.setOnClickListener {
            listener?.invoke(null)
        }
    }

    override fun onPurchased(deviceApp: DeviceApp) {
        var msgText = "Se ha restaurado su licencia. Puede usar la app sin limites."

        if (deviceApp.androidApp.status == ApplicationStatus.DISCONTINUED) {
            msgText += " Sin embargo la aplicación ha sido descontinuada, por lo que no recibirá más actualizaciones."
        }

        binding.message.text = msgText
        enableBtnAction {
            listener?.invoke(null)
        }
    }

    override fun onDiscontinued(deviceApp: DeviceApp) {
        binding.message.text = "La app ha sido descontinuada y no puede ser utilizada."

        enableBtnAction("Cerrar") {
            activity?.finishAffinity()
        }
    }

    override fun onDeprecated(deviceApp: DeviceApp) {
        binding.message.text = "Loco actualiza que estas en la pre-historia."

        enableBtnAction("Actualizar") {
            UpdateFragment(deviceApp.androidApp)
                .show(childFragmentManager, null)
        }
    }

    override fun onTrialPeriod(deviceApp: DeviceApp, isTrialPeriod: Boolean) {
        if (isTrialPeriod) {
            val days = deviceApp.androidApp.trialPeriod - deviceApp.daysInUse()

            binding.message.text = "Licencia en periodo de prueba. Días restantes: $days"

            enableBtnAction {
                listener?.invoke(null)
            }
        } else {
            binding.message.text = "El tiempo de prueba ha caducado. Para continuar debe comprar la app."

            enableBtnAction("Comprar") {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, PurchasedFragment())
                    .commit()
            }
            binding.btnLater.visibility = View.VISIBLE
        }
    }

    override fun onFailed(th: Throwable) {
        binding.message.text = "Ocurrió un error: " + th.message

        enableBtnAction("Reintentar") {
            viewModel.getApplicationStatus(this)
            binding.waiting = true
        }
    }

    private fun enableBtnAction(text: String = "Continuar", listener: (view: View) -> Unit) {
        binding.btnAction.text = text
        binding.btnAction.setOnClickListener(listener)
        binding.waiting = false
    }
}