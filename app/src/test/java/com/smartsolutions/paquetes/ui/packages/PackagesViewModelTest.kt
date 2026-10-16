package com.smartsolutions.paquetes.ui.packages

import app.cash.turbine.test
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.data.PackagesRules
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PackagesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val plan = mockk<DataPackage> { every { id } returns DataPackages.PackageId.DailyBag }
    private val sim = Sim("2", 1L, Networks.NETWORK_4G).apply { packages = listOf(plan) }

    private val simManager = mockk<ISimManager> { coEvery { getSim("2", true) } returns sim }
    private val dataPackageManager = mockk<IDataPackageManager>(relaxed = true)

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle(mapOf(PackagesViewModel.ARG_SIM_ID to "2"))) =
        PackagesViewModel(handle, simManager, dataPackageManager)

    @Test
    fun initialStateShowsThePlansOfTheLine() = runTest {
        viewModel().state.test {
            assertEquals(true, awaitItem().loading)

            val loaded = awaitItem()
            assertEquals(PackagesRules.Availability.AVAILABLE, loaded.availability)
            assertEquals(listOf(plan), loaded.plans)
        }
    }

    @Test
    fun purchaseStartsAndTheSharedSmsConfirmsItOnTheSameLine() = runTest {
        val handle = SavedStateHandle(mapOf(PackagesViewModel.ARG_SIM_ID to "2"))
        val viewModel = viewModel(handle)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPurchase(DataPackages.PackageId.DailyBag)
            assertEquals(PackagesEvent.PurchaseStarted, awaitItem())
        }
        coVerify { dataPackageManager.buyDataPackage(plan, sim) }

        // Tras la muerte del proceso, un ViewModel nuevo con el mismo estado guardado.
        val restored = viewModel(handle)
        restored.onSmsShared("sms")
        advanceUntilIdle()

        coVerify { dataPackageManager.confirmPurchaseFromSms("sms", "2") }
    }

    @Test
    fun missingPermissionAsksForIt() = runTest {
        coEvery { dataPackageManager.buyDataPackage(any(), any()) } throws MissingPermissionException("CALL_PHONE")
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onPurchase(DataPackages.PackageId.DailyBag)
            assertEquals(PackagesEvent.RequestPermission(IPermissionsManager.CALL_CODE), awaitItem())
        }
        assertEquals(false, viewModel.state.value.purchasing)
    }
}
