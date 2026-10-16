package com.smartsolutions.paquetes.ui.history

import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.Calendar

class HistoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun at(month: Int, day: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, month, day, 12, 0)
    }.timeInMillis

    private fun purchase(id: Long, date: Long, price: Float, simId: String, pending: Boolean = false) =
        PurchasedPackage(
            id, date, IDataPackageManager.ConnectionMode.USSD, simId, pending, DataPackages.PackageId.DailyBag
        ).apply {
            dataPackage = mockk<DataPackage> { every { this@mockk.price } returns price }
        }

    private val line1 = Sim("1", 1L, Networks.NETWORK_NONE)
    private val line2 = Sim("2", 1L, Networks.NETWORK_4G)

    private val simManager = mockk<ISimManager> {
        every { flowInstalledSims(any()) } returns flowOf(listOf(line1, line2))
    }

    private fun viewModel(history: kotlinx.coroutines.flow.Flow<List<PurchasedPackage>>) =
        HistoryViewModel(
            simManager,
            mockk<IPurchasedPackagesManager> { every { getHistory() } returns history }
        )

    @Test
    fun initialStateIsLoadingAndThenHasTheLinesWithTheirRowsByMonth() = runTest {
        val viewModel = viewModel(
            flowOf(
                listOf(
                    purchase(1, at(Calendar.SEPTEMBER, 3), 25f, "2"),
                    purchase(2, at(Calendar.OCTOBER, 1), 240f, "2"),
                    purchase(3, at(Calendar.OCTOBER, 9), 120f, "2")
                )
            )
        )

        assertEquals(true, viewModel.state.value.loading)

        val state = viewModel.state.first { !it.loading }

        assertEquals(listOf(line1, line2), state.sims)

        // Por mes, del más reciente al más antiguo: cabecera y compras.
        val rows = state.rows.getValue("2")
        assertEquals(5, rows.size)

        val octoberHeader = rows[0] as HistoryViewModel.HeaderPurchasedPackage
        assertEquals(360, octoberHeader.priceTotal)
        assertEquals(2, octoberHeader.cuantity)
        assertEquals(listOf(3L, 2L), listOf(rows[1].id, rows[2].id))

        val septemberHeader = rows[3] as HistoryViewModel.HeaderPurchasedPackage
        assertEquals(25, septemberHeader.priceTotal)
        assertEquals(1L, rows[4].id)
    }

    @Test
    fun aLineWithoutPurchasesHasNoRows() = runTest {
        val viewModel = viewModel(flowOf(listOf(purchase(1, at(Calendar.OCTOBER, 1), 25f, "2"))))

        val state = viewModel.state.first { !it.loading }

        assertTrue(state.rows.getValue("1").isEmpty())
    }

    @Test
    fun aConfirmedPurchaseShowsUpWithoutReopening() = runTest {
        val history = MutableSharedFlow<List<PurchasedPackage>>(replay = 1)
        history.tryEmit(listOf(purchase(1, at(Calendar.OCTOBER, 1), 25f, "2", pending = true)))
        val viewModel = viewModel(history)

        val before = viewModel.state.first { !it.loading }
        assertEquals(true, before.rows.getValue("2")[1].pending)

        history.tryEmit(listOf(purchase(1, at(Calendar.OCTOBER, 1), 25f, "2", pending = false)))

        val after = viewModel.state.first { it.rows["2"]?.getOrNull(1)?.pending == false }
        assertEquals(false, after.rows.getValue("2")[1].pending)
    }
}
