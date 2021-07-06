package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusFragment :
    AbstractSettingsFragment(R.layout.application_status_fragment),
    IActivationManager.ApplicationStatusListener
{

    private val viewModel by viewModels<ApplicationStatusViewModel>()

    private lateinit var container: FrameLayout

    override fun isRequired(): Boolean {
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        container = view.findViewById(R.id.container)

        view.findViewById<MaterialButton>(R.id.btn_action).setOnClickListener {
            viewModel.getApplicationStatus(this)
        }
    }

    override fun onPurchased(deviceApp: DeviceApp) {
        TODO("Not yet implemented")
    }

    override fun onDiscontinued(deviceApp: DeviceApp) {
        TODO("Not yet implemented")
    }

    override fun onDeprecated(deviceApp: DeviceApp) {
        TODO("Not yet implemented")
    }

    override fun onTrialPeriod(deviceApp: DeviceApp, isTrialPeriod: Boolean) {
        TODO("Not yet implemented")
    }

    override fun onFailed(th: Throwable) {
        container.removeAllViews()
        val view = layoutInflater.inflate(R.layout.failed_application_status, container, false)
        view.findViewById<MaterialTextView>(R.id.error_msg)
            .text = th.message
        container.addView(view)
    }
}