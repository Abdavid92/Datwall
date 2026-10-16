package com.smartsolutions.paquetes.ui.dashboard

import app.cash.turbine.test
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.helpers.BubbleServiceHelper
import com.smartsolutions.paquetes.helpers.FirewallHelper
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun app(access: Boolean) = App().apply { this.access = access }

    private val firewallOn = MutableStateFlow(false)
    private val bubbleOn = MutableStateFlow(false)
    private val preferences = MutableStateFlow(AppPreferences())

    private val appsManager = mockk<IAppsManager> {
        coEvery { all() } returns listOf(app(true), app(true), app(false))
    }
    private val permissionsManager = mockk<IPermissionsManager> {
        every { isGranted(any()) } returns true
    }
    private val firewallHelper = mockk<FirewallHelper>(relaxed = true) {
        every { observeFirewallState() } returns firewallOn
        every { allAccess(any()) } returns false
        every { checkFirewallPermission() } returns true
    }
    private val bubbleServiceHelper = mockk<BubbleServiceHelper>(relaxed = true) {
        every { observeBubbleChanges() } returns bubbleOn
    }
    private val ussdHelper = mockk<USSDHelper>(relaxed = true)
    private val preferencesRepository = mockk<IPreferencesRepository>(relaxed = true) {
        every { this@mockk.preferences } returns this@DashboardViewModelTest.preferences
    }

    private fun viewModel() = DashboardViewModel(
        appsManager, permissionsManager, firewallHelper, bubbleServiceHelper, ussdHelper, preferencesRepository
    )

    @Test
    fun initialStateHasCountsAndDefaults() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.allowedApps)
        assertEquals(1, state.blockedApps)
        assertEquals(3, state.totalApps)
        assertEquals(false, state.firewallOn)
        assertEquals(true, state.dynamicFirewall)
        assertEquals(DashboardViewModel.DEFAULT_BUBBLE_SIZE, state.bubbleSize)
        assertEquals(DashboardViewModel.DEFAULT_BUBBLE_TRANSPARENCY, state.bubbleTransparency)
    }

    @Test
    fun stateFollowsThePreferencesAndTheServices() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        firewallOn.value = true
        preferences.value = AppPreferences(dynamicFirewall = false, bubbleSize = "LARGE", bubbleTransparency = 0.8f)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(true, state.firewallOn)
        assertEquals(false, state.dynamicFirewall)
        assertEquals(2, state.bubbleSize)
        assertEquals(0.8f, state.bubbleTransparency)
    }

    @Test
    fun firewallStartsWhenThereAreBlockedAppsAndPermission() = runTest {
        val viewModel = viewModel()

        viewModel.onFirewallToggled(true)
        advanceUntilIdle()

        coVerify { firewallHelper.startFirewall(true) }
    }

    @Test
    fun firewallWithAllAppsAllowedShowsTheMessage() = runTest {
        every { firewallHelper.allAccess(any()) } returns true
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onFirewallToggled(true)
            assertEquals(DashboardEvent.FirewallNeedsBlockedApp, awaitItem())
        }
        coVerify(exactly = 0) { firewallHelper.startFirewall(any()) }
    }

    @Test
    fun firewallWithoutVpnPermissionAsksForItAndStartsWhenGranted() = runTest {
        every { firewallHelper.checkFirewallPermission() } returns false
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onFirewallToggled(true)
            assertEquals(DashboardEvent.RequestPermission(IPermissionsManager.VPN_CODE), awaitItem())
        }

        viewModel.onPermissionGranted(IPermissionsManager.VPN_CODE)
        advanceUntilIdle()

        coVerify { firewallHelper.startFirewall(true) }
    }

    @Test
    fun bubbleWithoutOverlayPermissionAsksForIt() = runTest {
        every { permissionsManager.isGranted(IPermissionsManager.DRAW_OVERLAYS_CODE) } returns false
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onBubbleToggled(true)
            assertEquals(DashboardEvent.RequestPermission(IPermissionsManager.DRAW_OVERLAYS_CODE), awaitItem())
        }
        coVerify(exactly = 0) { bubbleServiceHelper.startBubble(any()) }
    }

    @Test
    fun preferenceChangesAreSaved() = runTest {
        val viewModel = viewModel()

        viewModel.onDynamicFirewallToggled(false)
        viewModel.onBubbleSizeChanged(2)
        viewModel.onBubbleTransparencyChanged(0.3f)
        viewModel.onBubbleAlwaysShowChanged(false)
        advanceUntilIdle()

        coVerify { preferencesRepository.setDynamicFirewall(false) }
        coVerify { preferencesRepository.setBubbleSize("LARGE") }
        coVerify { preferencesRepository.setBubbleTransparency(0.3f) }
        coVerify { preferencesRepository.setBubbleAlwaysShow(false) }
    }

    @Test
    fun ussdShortcutWithoutCallPermissionAsksForIt() = runTest {
        coEvery { ussdHelper.sendUSSDRequestLegacy(any(), any()) } throws
            com.smartsolutions.paquetes.exceptions.USSDRequestException(USSDHelper.DENIED_CALL_PERMISSION)
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onUssdShortcut(DashboardViewModel.USSD_CREDIT)
            assertEquals(DashboardEvent.RequestPermission(IPermissionsManager.CALL_CODE), awaitItem())
        }
    }
}
