package com.smartsolutions.paquetes.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.transition.TransitionInflater
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.ui.activation.ApplicationStatusFragment
import dagger.Lazy
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

abstract class AbstractFragment: Fragment {

    constructor(): super()

    constructor(@LayoutRes contentLayoutId: Int): super(contentLayoutId)

    @Inject
    lateinit var activationManager: Lazy<IActivationManager>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val inflater = TransitionInflater.from(requireContext())
        enterTransition = inflater.inflateTransition(R.transition.fade_in)
        exitTransition = inflater.inflateTransition(R.transition.fade_out)
    }

    protected fun canWork(): Boolean {
        return runBlocking {
            activationManager.get().canWork().first
        }
    }

    protected fun inflatePurchasedFunctionLayout(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): View {
        val view = inflater.inflate(
            R.layout.purchase_function_layout,
            container,
            false
        )

        view.findViewById<Button>(R.id.btn_purchase).setOnClickListener {
            val intent = Intent(
                requireContext(),
                FragmentContainerActivity::class.java
            ).putExtra(
                FragmentContainerActivity.EXTRA_FRAGMENT,
                ApplicationStatusFragment::class.java.name
            )

            startActivity(intent)
        }

        view.findViewById<Toolbar>(R.id.toolbar)
            .title = getTitle()

        return view
    }

    protected open fun getTitle(): CharSequence {
        return requireContext().getString(R.string.app_name)
    }
}