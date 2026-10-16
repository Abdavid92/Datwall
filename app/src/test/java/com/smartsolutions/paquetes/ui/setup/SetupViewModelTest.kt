package com.smartsolutions.paquetes.ui.setup

import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.managers.models.ConfigurationStep
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SetupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val kernel = mockk<DatwallKernel>(relaxed = true)

    private fun configuration(step: ConfigurationStep) = Configuration(true, step) { false }

    private fun viewModel(vararg steps: ConfigurationStep): SetupViewModel {
        val manager = mockk<IConfigurationManager> {
            coEvery { getUncompletedConfigurations(any()) } returns steps.map { configuration(it) }.toTypedArray()
        }
        return SetupViewModel(manager, kernel)
    }

    @Test
    fun initialStateIsLoadingAndThenShowsTheFirstPendingStep() = runTest {
        val viewModel = viewModel(ConfigurationStep.SIMS, ConfigurationStep.PACKAGES)

        assertNull(viewModel.state.value.steps)
        assertNull(viewModel.state.value.current)

        advanceUntilIdle()

        assertEquals(listOf(ConfigurationStep.SIMS, ConfigurationStep.PACKAGES), viewModel.state.value.steps)
        assertEquals(ConfigurationStep.SIMS, viewModel.state.value.current)
    }

    @Test
    fun completingAStepShowsTheNextOneAndTheLastOneRestartsTheStart() = runTest {
        val viewModel = viewModel(ConfigurationStep.SIMS, ConfigurationStep.PACKAGES)
        advanceUntilIdle()

        viewModel.onStepCompleted()

        assertEquals(ConfigurationStep.PACKAGES, viewModel.state.value.current)
        verify(exactly = 0) { kernel.restart() }

        viewModel.onStepCompleted()

        verify(exactly = 1) { kernel.restart() }
    }

    @Test
    fun skippingTheSetupRestartsTheStartWithoutShowingTheRemainingSteps() = runTest {
        val viewModel = viewModel(ConfigurationStep.SIMS, ConfigurationStep.PACKAGES)
        advanceUntilIdle()

        viewModel.onSetupSkipped()

        verify(exactly = 1) { kernel.restart() }
    }

    @Test
    fun withoutPendingStepsTheStartContinuesRightAway() = runTest {
        val viewModel = viewModel()

        advanceUntilIdle()

        verify(exactly = 1) { kernel.restart() }
        assertNull(viewModel.state.value.current)
    }

    /**
     * Al girar el teléfono la pantalla vuelve a leer el estado: debe seguir en el mismo paso. Antes
     * el LiveData volvía a entregar su valor y la pantalla avanzaba, saltándose el paso actual.
     */
    @Test
    fun readingTheStateAgainAfterARotationKeepsTheCurrentStep() = runTest {
        val viewModel = viewModel(ConfigurationStep.SIMS, ConfigurationStep.PACKAGES)
        advanceUntilIdle()

        val first = viewModel.state.value.current
        val afterRotation = viewModel.state.value.current

        assertEquals(first, afterRotation)
        assertEquals(ConfigurationStep.SIMS, afterRotation)
        verify(exactly = 0) { kernel.restart() }
    }
}
