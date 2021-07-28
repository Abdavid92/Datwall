package com.smartsolutions.paquetes.ui.permissions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.smartsolutions.paquetes.helpers.AccessibilityServiceHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartAccessibilityServiceViewModel @Inject constructor(
    application: Application,
    private val accessibilityServiceHelper: AccessibilityServiceHelper
) : AndroidViewModel(application) {

    var listener: SinglePermissionFragment.SinglePermissionCallback? = null
        set(value) {
            if (value != null)
                field = value
        }

    private var settingWasOpen = false


    fun openAccessibilityServicesActivity() {
        accessibilityServiceHelper.openAccessibilityServicesActivity()
        settingWasOpen = true
    }

    fun checkAccessibilityService(fragment: StartAccessibilityServiceFragment): Boolean {

        //TODO: Pendiente a cambios
        if (settingWasOpen) {
            if (accessibilityServiceHelper.accessibilityServiceEnabled())
                listener?.onGranted()
            else
                listener?.onDenied()
            return true
        }
        return false
    }
}