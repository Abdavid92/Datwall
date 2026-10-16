package com.smartsolutions.paquetes.ui.permissions

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.helpers.AccessibilityServiceHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class StartAccessibilityServiceViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var enabled = false

    private val helper = mockk<AccessibilityServiceHelper>(relaxed = true) {
        every { accessibilityServiceEnabled() } answers { enabled }
    }

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        StartAccessibilityServiceViewModel(handle, helper)

    @Test
    fun openingTheSettingsAsksTheHelperAndWaitsForTheUserToComeBack() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onOpenSettingsClicked()

            verify(exactly = 1) { helper.openAccessibilityServicesActivity() }
            expectNoEvents()
        }
    }

    @Test
    fun comingBackWithTheServiceEnabledFinishesAsGranted() = runTest {
        val viewModel = viewModel()
        viewModel.onOpenSettingsClicked()

        enabled = true

        viewModel.events.test {
            viewModel.onResumed()
            org.junit.Assert.assertEquals(StartAccessibilityEvent.Finish(true), awaitItem())
        }
    }

    @Test
    fun comingBackWithoutEnablingTheServiceFinishesAsNotGranted() = runTest {
        val viewModel = viewModel()
        viewModel.onOpenSettingsClicked()

        viewModel.events.test {
            viewModel.onResumed()
            org.junit.Assert.assertEquals(StartAccessibilityEvent.Finish(false), awaitItem())
        }
    }

    @Test
    fun resumingWithoutHavingOpenedTheSettingsDoesNothing() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onResumed()
            expectNoEvents()
        }
    }

    @Test
    fun skippingFinishesAsNotGrantedOnceAndDismissingAfterwardsDoesNotReportAgain() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onSkipClicked()
            org.junit.Assert.assertEquals(StartAccessibilityEvent.Finish(false), awaitItem())
        }

        assertFalse(viewModel.onDismissed())
    }

    @Test
    fun dismissingWithoutAnAnswerReportsNotGrantedExactlyOnce() = runTest {
        val viewModel = viewModel()

        assertTrue(viewModel.onDismissed())
        assertFalse(viewModel.onDismissed())
    }

    /**
     * Si el usuario fue a los ajustes y el proceso se recrea, al volver se sigue comprobando.
     */
    @Test
    fun theWaitSurvivesRecreatingTheViewModel() = runTest {
        val handle = SavedStateHandle()
        viewModel(handle).onOpenSettingsClicked()
        enabled = true

        val recreated = viewModel(handle)

        recreated.events.test {
            recreated.onResumed()
            org.junit.Assert.assertEquals(StartAccessibilityEvent.Finish(true), awaitItem())
        }
    }
}
