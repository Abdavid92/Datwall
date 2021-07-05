package com.smartsolutions.paquetes.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ApplicationStatusFragment : AbstractSettingsFragment(R.layout.application_status_fragment) {

    private val viewModel by viewModels<ApplicationStatusViewModel>()

    override fun isRequired(): Boolean {
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }
}