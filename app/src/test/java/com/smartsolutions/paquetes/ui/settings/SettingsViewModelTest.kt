package com.smartsolutions.paquetes.ui.settings

import app.cash.turbine.test
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun clearHistoryDelegatesToTheManager() = runTest {
        val purchases = mockk<IPurchasedPackagesManager>(relaxed = true)
        val viewModel = SettingsViewModel(purchases, mockk<IIconManager>(relaxed = true))

        viewModel.clearHistory()
        advanceUntilIdle()

        coVerify(exactly = 1) { purchases.clearHistory() }
    }

    @Test
    fun clearIconCacheDelegatesToTheManager() = runTest {
        val icons = mockk<IIconManager>(relaxed = true)
        val viewModel = SettingsViewModel(mockk(relaxed = true), icons)

        viewModel.clearIconCache()
        advanceUntilIdle()

        coVerify(exactly = 1) { icons.deleteAll() }
    }

    private val sim = Sim("2", 1L, "4G")

    private val simManager = mockk<ISimManager>(relaxed = true) {
        io.mockk.coEvery { getInstalledSims() } returns listOf(sim)
    }
    private val preferences = mockk<IPreferencesRepository>(relaxed = true)

    @Test
    fun withoutCallPermissionTheLinesConfigurationAsksForIt() = runTest {
        val permissions = mockk<IPermissionsManager>(relaxed = true) {
            every { findPermission(IPermissionsManager.CALL_CODE) } returns null
        }

        val viewModel = SimsConfigurationViewModel(mockk(relaxed = true), permissions, simManager, preferences)
        advanceUntilIdle()

        // Sin definición del permiso se leen las líneas directamente.
        assertEquals(listOf(sim), viewModel.sims.value)
    }

    @Test
    fun deniedPermissionSkipsTheSetupAndRemembersIt() = runTest {
        val viewModel = SimsConfigurationViewModel(
            mockk(relaxed = true),
            mockk<IPermissionsManager>(relaxed = true) { every { findPermission(any()) } returns null },
            simManager,
            preferences
        )

        viewModel.events.test {
            viewModel.onPermissionDenied()
            assertEquals(SimsConfigurationEvent.SetupSkipped, awaitItem())
        }
        coVerify { preferences.setSimSetupSkipped(true) }
    }

    @Test
    fun savingSetsTheDefaultLinesAndCompletes() = runTest {
        val voice = Sim("1", 1L, "4G").apply { slotIndex = 0 }
        val data = Sim("2", 1L, "4G").apply { slotIndex = 1 }
        val viewModel = SimsConfigurationViewModel(
            mockk(relaxed = true),
            mockk<IPermissionsManager>(relaxed = true) { every { findPermission(any()) } returns null },
            simManager,
            preferences
        )

        viewModel.events.test {
            viewModel.onSave(data, voice)
            assertEquals(SimsConfigurationEvent.Saved, awaitItem())
        }
        coVerify { simManager.setDefaultSimManual(SimType.DATA, 1) }
        coVerify { simManager.setDefaultSimManual(SimType.VOICE, 0) }
    }
}
