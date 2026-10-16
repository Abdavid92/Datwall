package com.smartsolutions.paquetes.ui.setup

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commitNow
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.IReplaceFragments
import com.smartsolutions.paquetes.ui.followStartDestination
import com.smartsolutions.paquetes.ui.next
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import com.smartsolutions.paquetes.managers.models.ConfigurationStep
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SetupActivity : AbstractActivity(R.layout.activity_setup),
    OnCompletedListener, IReplaceFragments {

    private val viewModel by viewModels<SetupViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.configurations.observe(this) {
            nextOrComplete()
        }

        followStartDestination()
    }

    override fun onCompleted() {
        nextOrComplete()
    }

    override fun onSetupSkipped() {
        viewModel.next()
    }

    private fun nextOrComplete() {
        if (viewModel.hasNextConfiguration()) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.setup_container, screenOf(viewModel.nextConfiguration()!!.step), null)
                .commit()
        } else {
            viewModel.next()
        }
    }

    /**
     * Pantalla que muestra cada paso de la configuración.
     * */
    private fun screenOf(step: ConfigurationStep): Class<out AbstractSettingsFragment> = when (step) {
        ConfigurationStep.SIMS -> SimsConfigurationFragment::class.java
        ConfigurationStep.PACKAGES -> PackagesConfigurationFragment::class.java
    }

    override fun replace(fragment: AbstractSettingsFragment) {
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            replace(R.id.setup_container, fragment)
        }
    }
}