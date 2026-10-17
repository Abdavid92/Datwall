package com.smartsolutions.paquetes.ui.setup

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.managers.models.ConfigurationStep
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.IReplaceFragments
import com.smartsolutions.paquetes.ui.followStartDestination
import com.smartsolutions.paquetes.ui.settings.AbstractSettingsFragment
import com.smartsolutions.paquetes.ui.settings.PackagesConfigurationFragment
import com.smartsolutions.paquetes.ui.settings.SimsConfigurationFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SetupActivity : AbstractActivity(R.layout.activity_setup), IReplaceFragments {

    private val viewModel by viewModels<SetupViewModel>()

    /**
     * Último paso que se mostró. Se conserva al recrear la actividad: el gestor de fragmentos
     * ya restaura la pantalla de ese paso y no hay que volver a ponerla.
     * */
    private var shownStep: ConfigurationStep? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        shownStep = savedInstanceState?.getString(STATE_SHOWN_STEP)?.let(ConfigurationStep::valueOf)

        onSetupStepResult { skipped ->
            if (skipped)
                viewModel.onSetupSkipped()
            else
                viewModel.onStepCompleted()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.map { it.current }.distinctUntilChanged().collect(::show)
            }
        }

        followStartDestination()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_SHOWN_STEP, shownStep?.name)
    }

    private fun show(step: ConfigurationStep?) {
        if (step == null || step == shownStep)
            return

        shownStep = step

        supportFragmentManager.commit {
            replace(R.id.setup_container, screenOf(step), null)
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

    private companion object {
        const val STATE_SHOWN_STEP = "shown_step"
    }
}
