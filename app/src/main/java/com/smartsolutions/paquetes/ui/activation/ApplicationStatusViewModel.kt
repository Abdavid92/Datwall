package com.smartsolutions.paquetes.ui.activation

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ApplicationStatusViewModel @Inject constructor(
    private val activationManager: IActivationManager
) : ViewModel() {

    fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener) {
        activationManager.getApplicationStatus(listener)
    }
}