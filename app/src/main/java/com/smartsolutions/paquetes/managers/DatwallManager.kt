package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDatwallManager
import javax.inject.Inject

class DatwallManager @Inject constructor(
    private val ussdHelper: USSDHelper
) : IDatwallManager {

    override fun accessibilityServiceEnabled(): Boolean {
        return ussdHelper.accessibilityServiceEnabled()
    }

    override suspend fun startAccessibilityService(): Boolean {
        TODO("Not yet implemented")
    }
}