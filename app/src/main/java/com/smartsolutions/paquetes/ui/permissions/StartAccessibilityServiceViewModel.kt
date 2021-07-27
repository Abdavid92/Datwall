package com.smartsolutions.paquetes.ui.permissions

import androidx.lifecycle.ViewModel
import com.smartsolutions.paquetes.helpers.USSDHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartAccessibilityServiceViewModel @Inject constructor(
    private val ussdHelper: USSDHelper
) : ViewModel() {

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

    fun checkAccessibilityService(): Boolean {

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