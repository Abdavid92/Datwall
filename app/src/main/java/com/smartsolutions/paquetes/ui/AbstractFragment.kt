package com.smartsolutions.paquetes.ui

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.smartsolutions.paquetes.R

abstract class AbstractFragment: Fragment {

    constructor(): super()

    constructor(@LayoutRes contentLayoutId: Int): super(contentLayoutId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade_in)
        exitTransition = inflater.inflateTransition(R.transition.fade_out)
    }

    protected open fun getTitle(): CharSequence {
        return requireContext().getString(R.string.app_name)
    }
}