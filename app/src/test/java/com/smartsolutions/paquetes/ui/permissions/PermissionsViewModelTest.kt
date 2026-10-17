package com.smartsolutions.paquetes.ui.permissions

import android.content.Context
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
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PermissionsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val grantedCodes = mutableSetOf<Int>()
    private val requested = mutableSetOf<Int>()

    private fun permission(code: Int, atStartup: Boolean) = Permission(
        arrayOf("android.permission.TEST$code"),
        code,
        23,
        PermissionKind.RUNTIME,
        atStartup,
        checkPermission = { grantedCodes.contains(code) }
    )

    private val startup = permission(IPermissionsManager.CALL_CODE, true)
    private val other = permission(IPermissionsManager.DRAW_OVERLAYS_CODE, false)

    private val permissionsManager = mockk<IPermissionsManager> {
        every { permissions } returns listOf(startup, other)
        coEvery { wasRequested(any()) } answers { requested.contains(firstArg()) }
        coEvery { markRequested(any()) } answers { requested.add(firstArg()) }
    }

    private val preferences = mockk<IPreferencesRepository>(relaxed = true)
    private val kernel = mockk<DatwallKernel>(relaxed = true)
    private val context = mockk<Context>()

    private fun configurations(pending: Boolean) = mockk<IConfigurationManager> {
        coEvery { getUncompletedConfigurations(any()) } returns
            if (pending) arrayOf(Configuration(true, ConfigurationStep.SIMS) { false }) else emptyArray()
    }

    private fun viewModel(pendingConfigurations: Boolean = false) = PermissionsViewModel(
        permissionsManager, configurations(pendingConfigurations), preferences, kernel, context
    )

    @Test
    fun theFirstUseShowsOnlyTheStartupPermissionsAndSettingsShowsAll() = runTest {
        val first = viewModel()
        first.onShown(firstUse = true, rationaleKeys = emptySet())
        advanceUntilIdle()

        assertEquals(listOf(startup), first.state.value.items.map { it.permission })

        val settings = viewModel()
        settings.onShown(firstUse = false, rationaleKeys = emptySet())
        advanceUntilIdle()

        assertEquals(listOf(startup, other), settings.state.value.items.map { it.permission })
    }

    @Test
    fun theActionOfEachRowDependsOnGrantHistoryAndRationale() = runTest {
        grantedCodes.add(startup.requestCode)
        requested.add(other.requestCode)

        val viewModel = viewModel()
        viewModel.onShown(firstUse = false, rationaleKeys = emptySet())
        advanceUntilIdle()

        // Concedido: nada. Pedido antes y sin rationale: rechazado para siempre.
        assertEquals(
            listOf(PermissionAction.Nothing, PermissionAction.OpenAppSettings),
            viewModel.state.value.items.map { it.action }
        )

        viewModel.onShown(firstUse = false, rationaleKeys = other.keys.toSet())
        advanceUntilIdle()

        assertEquals(PermissionAction.Request, viewModel.state.value.items[1].action)
    }

    @Test
    fun finishingTheFirstUseSavesTheAnswerAndRestartsTheStart() = runTest {
        val viewModel = viewModel()

        viewModel.onFinishFirstUse()
        advanceUntilIdle()

        coVerify { preferences.setDataDisclosureAnswered(true) }
        verify { kernel.restart() }
    }

    @Test
    fun clickingARowThatCanBeRequestedMarksItAndAsksTheSystem() = runTest {
        val viewModel = viewModel()
        viewModel.onShown(firstUse = true, rationaleKeys = emptySet())
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPermissionClicked(viewModel.state.value.items.single())

            assertEquals(PermissionsEvent.RequestFromSystem(startup), awaitItem())
            assertTrue(requested.contains(startup.requestCode))
        }
    }

    @Test
    fun clickingARejectedForeverRowOpensTheAppSettings() = runTest {
        requested.add(startup.requestCode)
        val viewModel = viewModel()
        viewModel.onShown(firstUse = true, rationaleKeys = emptySet())
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPermissionClicked(viewModel.state.value.items.single())

            assertEquals(PermissionsEvent.OpenAppSettings, awaitItem())
        }
    }

    @Test
    fun theRowChangesItsActionAfterASystemResult() = runTest {
        val viewModel = viewModel()
        viewModel.onShown(firstUse = true, rationaleKeys = emptySet())
        advanceUntilIdle()

        assertEquals(PermissionAction.Request, viewModel.state.value.items.single().action)

        grantedCodes.add(startup.requestCode)
        viewModel.onSystemResult(startup.requestCode, true, emptySet())
        advanceUntilIdle()

        assertEquals(PermissionAction.Nothing, viewModel.state.value.items.single().action)
    }

    @Test
    fun callPermissionGrantedWithPendingLinesRestartsAndAsksToOpenTheStart() = runTest {
        val viewModel = viewModel(pendingConfigurations = true)
        viewModel.onShown(firstUse = false, rationaleKeys = emptySet())
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onSystemResult(IPermissionsManager.CALL_CODE, true, emptySet())

            assertEquals(PermissionsEvent.SimSetupPending, awaitItem())
        }
        verify { kernel.restart() }
    }

    @Test
    fun callPermissionGrantedDuringTheFirstUseDoesNotRestart() = runTest {
        val viewModel = viewModel(pendingConfigurations = true)
        viewModel.onShown(firstUse = true, rationaleKeys = emptySet())
        advanceUntilIdle()

        viewModel.onSystemResult(IPermissionsManager.CALL_CODE, true, emptySet())
        advanceUntilIdle()

        verify(exactly = 0) { kernel.restart() }
    }
}
