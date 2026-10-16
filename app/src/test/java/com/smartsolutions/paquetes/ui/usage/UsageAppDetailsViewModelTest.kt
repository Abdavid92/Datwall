package com.smartsolutions.paquetes.ui.usage

import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.managers.NetworkUsageManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class UsageAppDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val traffics = listOf(Traffic(10, 100, 0, "2"))

    private val dateCalendarUtils = mockk<DateCalendarUtils> {
        coEvery { getTimePeriod(any()) } returns (0L to 1L)
    }
    private val networkUsageManager = mockk<NetworkUsageManager> {
        coEvery { getAppUsageByLapsusTime(any(), any(), any(), any()) } returns traffics
    }

    private fun viewModel(stored: Int?): UsageAppDetailsViewModel {
        val preferences = mockk<IPreferencesRepository> {
            coEvery { awaitLoaded() } returns AppPreferences(usagePeriod = stored)
        }
        return UsageAppDetailsViewModel(dateCalendarUtils, networkUsageManager, preferences)
    }

    /**
     * Si el usuario nunca cambió el periodo no hay valor guardado: antes la gráfica salía vacía.
     */
    @Test
    fun withoutAStoredPeriodItShowsTodayInsteadOfAnEmptyChart() = runTest {
        val viewModel = viewModel(stored = null)

        assertNull(viewModel.details.value)

        viewModel.load(10)
        advanceUntilIdle()

        val details = viewModel.details.value!!
        assertEquals(traffics, details.traffics)
        assertEquals(DateCalendarUtils.MyTimeUnit.HOUR, details.timeUnit)
        coVerify { dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_TODAY) }
    }

    @Test
    fun usesTheStoredPeriod() = runTest {
        val viewModel = viewModel(stored = DateCalendarUtils.PERIOD_MONTH)

        viewModel.load(10)
        advanceUntilIdle()

        assertEquals(DateCalendarUtils.MyTimeUnit.DAY, viewModel.details.value!!.timeUnit)
        coVerify { dateCalendarUtils.getTimePeriod(DateCalendarUtils.PERIOD_MONTH) }
    }
}
