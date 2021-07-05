package com.smartsolutions.paquetes.ui.settings

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ApplicationStatusViewModel @Inject constructor(
    private val activationManager: IActivationManager
) : ViewModel(), IActivationManager.ApplicationStatusListener {

    fun getApplicationStatus() {
        activationManager.getApplicationStatus(this)
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
        TODO("Not yet implemented")
    }
}