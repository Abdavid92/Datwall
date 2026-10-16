package com.smartsolutions.paquetes.ui.usage

import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.managers.models.UsageOrder
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import com.smartsolutions.paquetes.repositories.models.TrafficType
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

class UsageViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun app(name: String, uid: Int, type: TrafficType = TrafficType.International) = App().apply {
        packageName = "pkg.$name"
        this.uid = uid
        this.name = name
        trafficType = type
    }

    private val preferences = MutableStateFlow(AppPreferences(usagePeriod = 2, usageFilter = UsageOrder.MAX_USAGE.name))

    private val networkUsageManager = mockk<NetworkUsageManager> {
        coEvery { getAppsUsage(any(), any()) } returns listOf(
            Traffic(1, 1000, 0, "2"),
            Traffic(2, 5, 0, "2"),
            Traffic(3, 300, 0, "2")
        )
    }
    private val appsManager = mockk<IAppsManager> {
        coEvery { byUids(any()) } answers {
            listOf(app("big", 1), app("tiny", 2), app("national", 3, TrafficType.National))
        }
    }
    private val simManager = mockk<ISimManager>(relaxed = true)
    private val dateCalendarUtils = mockk<DateCalendarUtils> { coEvery { getTimePeriod(any()) } returns (0L to 1L) }
    private val preferencesRepository = mockk<IPreferencesRepository>(relaxed = true) {
        every { this@mockk.preferences } returns this@UsageViewModelTest.preferences
    }

    private fun viewModel() = UsageViewModel(
        networkUsageManager, simManager, appsManager, dateCalendarUtils, preferencesRepository, mockk()
    )

    @Test
    fun initialStateHasTheRememberedPeriodAndTheBreakdownOfEachType() = runTest {
        val viewModel = viewModel()
        assertEquals(2, viewModel.state.value.period)
        assertEquals(true, viewModel.state.value.loading)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.loading)

        val international = state.breakdowns.getValue(TrafficType.International)
        assertEquals(1005L, international.total)
        assertEquals(listOf("big"), international.main.map { it.app.name })
        assertEquals(listOf("tiny"), international.others.map { it.app.name })
        assertEquals(listOf(1000L to "big", 5L to null), international.slices.map { it.bytes to it.label })
        assertEquals(UsageViewModel.OTHERS_COLOUR, international.slices.last().colour)

        assertEquals(300L, state.breakdowns.getValue(TrafficType.National).total)
    }

    @Test
    fun changingThePeriodIsRememberedAndReloads() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onPeriodChanged(0)
        preferences.value = AppPreferences(usagePeriod = 0)
        advanceUntilIdle()

        coVerify { preferencesRepository.setUsagePeriod(0) }
        assertEquals(0, viewModel.state.value.period)
        coVerify { dateCalendarUtils.getTimePeriod(0) }
    }
}
