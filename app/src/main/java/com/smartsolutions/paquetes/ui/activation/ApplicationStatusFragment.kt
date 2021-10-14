package com.smartsolutions.paquetes.ui.activation

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.github.razir.progressbutton.attachTextChangeAnimator
import com.github.razir.progressbutton.bindProgressButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.databinding.FragmentApplicationStatusBinding
import com.smartsolutions.paquetes.managers.contracts.IActivationManager2
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import com.smartsolutions.paquetes.ui.settings.UpdateFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusFragment : AbstractSettingsFragment(),
    IActivationManager2.ApplicationStatusListener
{

    private val viewModel by viewModels<ApplicationStatusViewModel>()

    private lateinit var binding: FragmentApplicationStatusBinding

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

        //binding.waiting = false

        viewLifecycleOwner.bindProgressButton(binding.btnAction)
        binding.btnAction.attachTextChangeAnimator()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAction.setOnClickListener {
            if (viewModel.getApplicationStatus(this))
                waiting(true)
                //binding.waiting = true
        }
        binding.btnLater.setOnClickListener {
            complete()
        }
    }

    override fun onPurchased(license: License) {
        var msgText = "Se ha restaurado su licencia. Puede usar la app sin limites."

        if (license.androidApp.status == ApplicationStatus.DISCONTINUED) {
            msgText += " Sin embargo la aplicación ha sido descontinuada, por lo que no recibirá más actualizaciones."
        }

        binding.message.text = msgText
        enableBtnAction {
            complete()
        }
    }

    override fun onDiscontinued(license: License) {
        binding.message.text = "La app ha sido descontinuada y no puede ser utilizada."

        enableBtnAction("Cerrar") {
            activity?.finishAffinity()
        }
    }

    override fun onDeprecated(license: License) {
        binding.message.text = "Loco actualiza que estas en la pre-historia."

        enableBtnAction("Actualizar") {
            UpdateFragment(license.androidApp)
                .show(childFragmentManager, null)
        }
    }

    override fun onTrialPeriod(license: License, isTrialPeriod: Boolean) {
        if (isTrialPeriod) {
            val days = license.androidApp.trialPeriod - license.daysInUse()

            binding.message.text = "Licencia en periodo de prueba. Días restantes: $days"

            enableBtnAction {
                complete()
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

    override fun onTooMuchOld(license: License) {

    }

    override fun onFailed(th: Throwable) {
        binding.message.text = "Ocurrió un error: " + th.message

        enableBtnAction("Reintentar") {
            viewModel.getApplicationStatus(this)
            waiting(true, "Reintentar")
            //binding.waiting = true
        }
    }

    private fun enableBtnAction(text: String = "Continuar", listener: (view: View) -> Unit) {
        binding.btnAction.text = text
        binding.btnAction.setOnClickListener(listener)
        waiting(false, text)
        //binding.waiting = false
    }

    private fun waiting(waiting: Boolean, text: String = getString(R.string.btn_continue)) {
        if (waiting) {
            binding.btnAction.showProgress {
                progressColor = Color.WHITE
            }
            binding.btnAction.isEnabled = false
        } else {
            binding.btnAction.hideProgress(text)
            binding.btnAction.isEnabled = true
        }
    }
}