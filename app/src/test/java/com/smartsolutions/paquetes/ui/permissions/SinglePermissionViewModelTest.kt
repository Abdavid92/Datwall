package com.smartsolutions.paquetes.ui.permissions

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.smartsolutions.paquetes.DatwallKernel
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.managers.PermissionAction
import com.smartsolutions.paquetes.managers.contracts.IConfigurationManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.models.Configuration
import com.smartsolutions.paquetes.managers.models.ConfigurationStep
import com.smartsolutions.paquetes.managers.models.Permission
import com.smartsolutions.paquetes.managers.models.PermissionKind
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SinglePermissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private var granted = false
    private var requestedBefore = false

    private fun permission(code: Int) = Permission(
        arrayOf("android.permission.TEST"),
        code,
        23,
        PermissionKind.RUNTIME,
        checkPermission = { granted }
    )

    private val kernel = mockk<DatwallKernel>(relaxed = true)
    private val context = mockk<Context>()

    private fun configurations(pending: Boolean) = mockk<IConfigurationManager> {
        coEvery { getUncompletedConfigurations(any()) } returns
            if (pending) arrayOf(Configuration(true, ConfigurationStep.SIMS) { false }) else emptyArray()
    }

    private fun permissionsManager(code: Int) = mockk<IPermissionsManager> {
        every { findPermission(code) } returns permission(code)
        coEvery { wasRequested(code) } answers { requestedBefore }
        coEvery { markRequested(code) } answers { requestedBefore = true }
    }

    private fun viewModel(
        code: Int = IPermissionsManager.DRAW_OVERLAYS_CODE,
        pendingConfigurations: Boolean = false,
        handle: SavedStateHandle = SavedStateHandle(mapOf(SinglePermissionViewModel.ARG_REQUEST_CODE to code))
    ) = SinglePermissionViewModel(
        handle, permissionsManager(code), configurations(pendingConfigurations), kernel, context
    )

    @Test
    fun theActionIsUnknownUntilTheScreenIsShownAndThenDependsOnTheHistory() = runTest {
        val viewModel = viewModel()

        assertNull(viewModel.state.value.action)

        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        // Nunca se pidió: se puede pedir aunque el sistema no deje volver a preguntar.
        assertEquals(PermissionAction.Request, viewModel.state.value.action)
    }

    @Test
    fun aPermissionAlreadyGrantedFinishesAsGranted() = runTest {
        granted = true
        val viewModel = viewModel()
        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        assertEquals(PermissionAction.Nothing, viewModel.state.value.action)

        viewModel.events.test {
            viewModel.onGrantClicked()
            assertEquals(SinglePermissionEvent.Finish(true), awaitItem())
        }
    }

    @Test
    fun grantingAsksTheSystemAndTheResultFinishesTheSheet() = runTest {
        val viewModel = viewModel()
        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onGrantClicked()
            assertEquals(SinglePermissionEvent.RequestFromSystem, awaitItem())

            viewModel.onRequestPermissionsResult(granted = true)
            assertEquals(SinglePermissionEvent.Finish(true), awaitItem())
        }
    }

    @Test
    fun rejectingFinishesAsNotGranted() = runTest {
        val viewModel = viewModel()
        viewModel.onShown(canAskAgain = true)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onGrantClicked()
            assertEquals(SinglePermissionEvent.RequestFromSystem, awaitItem())

            viewModel.onRequestPermissionsResult(granted = false)
            assertEquals(SinglePermissionEvent.Finish(false), awaitItem())
        }
    }

    @Test
    fun aPermanentlyRejectedPermissionSendsTheUserToTheSettingsAndChecksOnReturn() = runTest {
        requestedBefore = true
        val viewModel = viewModel()
        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        assertEquals(PermissionAction.OpenAppSettings, viewModel.state.value.action)

        viewModel.events.test {
            viewModel.onGrantClicked()
            assertEquals(SinglePermissionEvent.OpenAppSettings, awaitItem())

            // El usuario concedió el permiso en los ajustes.
            granted = true
            viewModel.onResumed()
            assertEquals(SinglePermissionEvent.Finish(true), awaitItem())
        }
    }

    @Test
    fun resumingWithoutHavingGoneToTheSettingsDoesNothing() = runTest {
        val viewModel = viewModel()
        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onResumed()
            expectNoEvents()
        }
    }

    @Test
    fun theCallPermissionWithPendingLinesRestartsTheStartInsteadOfReportingGranted() = runTest {
        val viewModel = viewModel(code = IPermissionsManager.CALL_CODE, pendingConfigurations = true)
        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onGrantClicked()
            assertEquals(SinglePermissionEvent.RequestFromSystem, awaitItem())

            viewModel.onRequestPermissionsResult(granted = true)
            assertEquals(SinglePermissionEvent.SimSetupPending, awaitItem())
        }
        verify(exactly = 1) { kernel.restart() }
    }

    @Test
    fun theCallPermissionWithoutPendingLinesReportsGranted() = runTest {
        val viewModel = viewModel(code = IPermissionsManager.CALL_CODE, pendingConfigurations = false)
        viewModel.onShown(canAskAgain = false)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onGrantClicked()
            awaitItem()
            viewModel.onRequestPermissionsResult(granted = true)
            assertEquals(SinglePermissionEvent.Finish(true), awaitItem())
        }
        verify(exactly = 0) { kernel.restart() }
    }

    @Test
    fun skippingFinishesAsNotGrantedOnceAndDismissingAfterwardsDoesNotReportAgain() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onSkipClicked()
            assertEquals(SinglePermissionEvent.Finish(false), awaitItem())
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
     * Si la pantalla se recrea después de entregar el resultado (por ejemplo, al girar), el ViewModel
     * nuevo no debe entregar un "denegado" más al descartarse.
     */
    @Test
    fun recreatingTheViewModelAfterTheResultDoesNotReportAnotherOne() = runTest {
        val handle = SavedStateHandle(
            mapOf(SinglePermissionViewModel.ARG_REQUEST_CODE to IPermissionsManager.DRAW_OVERLAYS_CODE)
        )
        val first = viewModel(handle = handle)
        first.onSkipClicked()
        advanceUntilIdle()

        val recreated = viewModel(handle = handle)

        assertFalse(recreated.onDismissed())
    }
}
