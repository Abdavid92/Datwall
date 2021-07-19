package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusFragment :
    AbstractSettingsFragment(R.layout.fragment_application_status),
    IActivationManager.ApplicationStatusListener
{

    private val viewModel by viewModels<ApplicationStatusViewModel>()

    private lateinit var message: MaterialTextView
    private lateinit var btnAction: MaterialButton

    override fun isRequired(): Boolean {
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        message = view.findViewById(R.id.message)
        btnAction = view.findViewById(R.id.btn_action)

        btnAction.setOnClickListener {
            viewModel.getApplicationStatus(this)
            it.isEnabled = false
        }
    }

    override fun onPurchased(deviceApp: DeviceApp) {
        var msgText = "Se ha restaurado su licencia. Puede usar la app sin limites."

        if (deviceApp.androidApp.status == ApplicationStatus.DISCONTINUED) {
            msgText += " Sin embargo la aplicación ha sido descontinuada, por lo que no recibirá más actualizaciones."
        }

        message.text = msgText
        enableBtnAction {
            listener?.invoke(null)
        }
    }

    override fun onDiscontinued(deviceApp: DeviceApp) {
        message.text = "La app ha sido descontinuada y no puede ser utilizada."

        enableBtnAction("Cerrar") {
            activity?.finishAffinity()
        }
    }

    override fun onDeprecated(deviceApp: DeviceApp) {
        message.text = "Loco actualiza que estas en la pre-historia."

        enableBtnAction("Actualizar") {
            listener?.invoke(null /*TODO:Fragmento de actualización*/)
        }
    }

    override fun onTrialPeriod(deviceApp: DeviceApp, isTrialPeriod: Boolean) {
        if (isTrialPeriod) {
            val days = deviceApp.androidApp.trialPeriod - deviceApp.daysInUse()

            message.text = "Licencia en periodo de prueba. Días restantes: $days"

            enableBtnAction {
                listener?.invoke(null)
            }
        } else {
            message.text = "El tiempo de prueba ha caducado. Para continuar debe comprar la app."

            enableBtnAction("Comprar") {
                listener?.invoke(/*TODO:Fragmento de compra*/ null)
            }
        }
    }

    override fun onFailed(th: Throwable) {
        message.text = "Ocurrió un error: " + th.message

        enableBtnAction("Reintentar") {
            viewModel.getApplicationStatus(this)
            it.isEnabled = false
        }
    }

    private fun enableBtnAction(text: String = "Continuar", listener: (view: View) -> Unit) {
        btnAction.text = text
        btnAction.setOnClickListener(listener)
        btnAction.isEnabled = true
    }
}