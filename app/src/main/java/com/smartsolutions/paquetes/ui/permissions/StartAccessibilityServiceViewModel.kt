package com.smartsolutions.paquetes.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartsolutions.paquetes.helpers.USSDHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class StartAccessibilityServiceViewModel @Inject constructor(
    application: Application,
    private val ussdHelper: USSDHelper
) : AndroidViewModel(application) {

    var listener: SinglePermissionFragment.SinglePermissionCallback? = null
        set(value) {
            if (value != null)
                field = value
        }

    private var settingWasOpen = false


    fun openAccessibilityServicesActivity() {
        ussdHelper.openAccessibilityServicesActivity()
        settingWasOpen = true
    }

    fun checkAccessibilityService(fragment: StartAccessibilityServiceFragment): Boolean {

        //TODO: Pendiente a cambios
        if (settingWasOpen) {
            if (ussdHelper.accessibilityServiceEnabled())
                listener?.onGranted()
            else
                listener?.onDenied()
            return true
        }
        return false
    }
}