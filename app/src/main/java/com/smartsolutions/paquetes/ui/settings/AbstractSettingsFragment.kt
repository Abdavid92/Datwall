package com.smartsolutions.paquetes.ui.settings

import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.ui.setup.OnCompletedListener
import kotlin.reflect.KClass

abstract class AbstractSettingsFragment : Fragment {

    constructor(): super()

    constructor(@LayoutRes contentLayoutId: Int): super(contentLayoutId)

    open fun complete() {
        activity?.let {
            if (it is OnCompletedListener)
                it.onCompleted()
        }
    }
}