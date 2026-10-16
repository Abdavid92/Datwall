package com.smartsolutions.paquetes.ui.settings

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.ui.setup.sendSetupStepResult
import kotlin.reflect.KClass

abstract class AbstractSettingsFragment : Fragment {

    constructor(): super()

    constructor(@LayoutRes contentLayoutId: Int): super(contentLayoutId)

    open fun complete() {
        sendSetupStepResult(skipped = false)
    }

    /**
     * Termina la configuración inicial sin completar los pasos restantes.
     * */
    fun skipSetup() {
        sendSetupStepResult(skipped = true)
    }
}