package com.smartsolutions.paquetes.ui.settings

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class AbstractSettingsFragment : Fragment {

    protected var listener: ((next: AbstractSettingsFragment?) -> Unit)? = null

    constructor(): super()

    constructor(@LayoutRes contentLayoutId: Int): super(contentLayoutId)

    /**
     * Indica si este fragmento es requerido por las configuraciones.
     * En caso contrario, se puede saltar como configuración.
     * */
    abstract fun isRequired(): Boolean

    fun setOnCompletedListener(listener: (next: AbstractSettingsFragment?) -> Unit):
            AbstractSettingsFragment {
        this.listener = listener
        return this
    }
}