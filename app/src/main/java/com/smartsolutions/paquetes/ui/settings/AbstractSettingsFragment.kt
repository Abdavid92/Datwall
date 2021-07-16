package com.smartsolutions.paquetes.ui.settings

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

abstract class AbstractSettingsFragment : Fragment {

    /**
     * Listener que se invoca cuando el fragmento termina su trabajo.
     * Si el argumento next no es null, indica que se debe instanciar un fragmento diferente al
     * predeterminado.
     * */
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