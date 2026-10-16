package com.smartsolutions.paquetes.ui.resume

import app.cash.turbine.test
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IPermissionsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IStatisticsManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.UserDataOrder
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResumeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sim = Sim("2", 1L, Networks.NETWORK_4G)
    private val future = System.currentTimeMillis() + 10 * 24 * 60 * 60 * 1000L

    private val small = UserDataBytes("2", DataType.International, 400, 100, 0, future)
    private val big = UserDataBytes("2", DataType.PromoBonus, 400, 300, 0, future)

    private val preferences = MutableStateFlow(AppPreferences(resumeFilter = UserDataOrder.SIZE_DESC.name))

    private val simManager = mockk<ISimManager> { every { flowInstalledSims() } returns flowOf(listOf(sim)) }
    private val userDataManager = mockk<IUserDataManager> { every { flowUserData("2") } returns flowOf(listOf(small, big)) }
    private val synchronizationManager = mockk<ISynchronizationManager>(relaxed = true)
    private val statisticsManager = mockk<IStatisticsManager> {
        coEvery { getAverage(any(), any(), any()) } returns DataUnitBytes(10)
        every { getRemainder(any(), any<List<UserDataBytes>>()) } returns DataUnitBytes(20)
    }
    private val preferencesRepository = mockk<IPreferencesRepository>(relaxed = true) {
        every { this@mockk.preferences } returns this@ResumeViewModelTest.preferences
    }

    private fun viewModel() = ResumeViewModel(
        simManager, userDataManager, synchronizationManager, statisticsManager, preferencesRepository
    )

    @Test
    fun initialStateHasTheLinesAndTheRememberedOrder() = runTest {
        val viewModel = viewModel()
        viewModel.onLineShown("2")

        // Las líneas llegan por el hilo de E/S.
        val state = viewModel.state.first { it.sims.isNotEmpty() && it.lines["2"]?.dailyRemainder != null }
        assertEquals(listOf(sim), state.sims)
        assertEquals(UserDataOrder.SIZE_DESC, state.order)

        val line = state.lines.getValue("2")
        assertEquals(listOf(big, small), line.data)
        assertEquals(400L, line.summary.restBytes)
        assertEquals(10L, line.dailyAverage!!.bytes)
        assertEquals(20L, line.dailyRemainder!!.bytes)
    }

    /**
     * `UserDataBytes.equals` solo compara tipo y línea: un cambio de megas del mismo tipo no debe
     * perderse por parecer "el mismo estado".
     */
    @Test
    fun changesInTheBytesOfTheSameTypesReachTheState() = runTest {
        // Como Room: emite cada lectura aunque sea "igual" por equals (un StateFlow la descartaría).
        val flow = MutableSharedFlow<List<UserDataBytes>>(replay = 1)
        flow.tryEmit(listOf(small, big))
        every { userDataManager.flowUserData("2") } returns flow
        val viewModel = viewModel()
        viewModel.onLineShown("2")
        viewModel.state.first { it.lines["2"]?.data?.size == 2 }

        flow.tryEmit(
            listOf(
                UserDataBytes("2", DataType.International, 400, 100, 0, future),
                UserDataBytes("2", DataType.PromoBonus, 400, 50, 0, future)
            )
        )

        val state = viewModel.state.first { it.lines["2"]?.data?.any { d -> d.bytes == 50L } == true }
        assertEquals(150L, state.lines.getValue("2").summary.restBytes)
    }

    @Test
    fun changingTheOrderReordersAndIsRemembered() = runTest {
        val viewModel = viewModel()
        viewModel.onLineShown("2")
        viewModel.state.first { it.lines["2"] != null }

        viewModel.onFilterChanged(UserDataOrder.SIZE_ASC)
        preferences.value = AppPreferences(resumeFilter = UserDataOrder.SIZE_ASC.name)
        val state = viewModel.state.first { it.order == UserDataOrder.SIZE_ASC }

        coVerify { preferencesRepository.setResumeFilter("SIZE_ASC") }
        assertEquals(listOf(small, big), state.lines.getValue("2").data)
    }

    @Test
    fun syncWithoutCallPermissionAsksForItAndRetriesWhenGranted() = runTest {
        coEvery { synchronizationManager.synchronizeUserDataBytes(sim) } throws
            USSDRequestException(USSDHelper.DENIED_CALL_PERMISSION) andThen Unit
        val viewModel = viewModel()
        viewModel.state.first { it.sims.isNotEmpty() }

        viewModel.events.test {
            viewModel.onSyncClicked("2")
            assertEquals(ResumeEvent.RequestPermission(IPermissionsManager.CALL_CODE), awaitItem())
        }
        assertEquals(false, viewModel.state.value.synchronizing)

        viewModel.onPermissionGranted()
        advanceUntilIdle()

        coVerify(exactly = 2) { synchronizationManager.synchronizeUserDataBytes(sim) }
    }

    @Test
    fun otherUssdErrorsAreReportedWithTheirCode() = runTest {
        coEvery { synchronizationManager.synchronizeUserDataBytes(sim) } throws
            USSDRequestException(USSDHelper.USSD_TOO_MANY_REQUESTS)
        val viewModel = viewModel()
        viewModel.state.first { it.sims.isNotEmpty() }

        viewModel.events.test {
            viewModel.onSyncClicked("2")
            assertEquals(ResumeEvent.SyncFailed(USSDHelper.USSD_TOO_MANY_REQUESTS), awaitItem())
        }
    }
}
