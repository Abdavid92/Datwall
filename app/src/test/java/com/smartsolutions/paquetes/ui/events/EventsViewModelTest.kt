package com.smartsolutions.paquetes.ui.events

import app.cash.turbine.test
import com.abdavid92.persistentlog.Event
import com.abdavid92.persistentlog.EventType
import com.abdavid92.persistentlog.LogManager
import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.helpers.LocalFileHelper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Date

class EventsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun event(time: Long, type: EventType, tag: String) = Event(Date(time), type, tag, "mensaje $tag", null)

    private val old = event(1_000, EventType.Error, "viejo")
    private val middle = event(2_000, EventType.Info, "medio")
    private val recent = event(3_000, EventType.Warning, "reciente")

    private val logManager = mockk<LogManager> {
        // El registro entrega los eventos sin orden.
        every { flow() } returns flowOf(listOf(middle, old, recent))
    }
    private val localFileHelper = mockk<LocalFileHelper>()

    private fun viewModel() = EventsViewModel(logManager, localFileHelper)

    @Test
    fun initialStateIsLoadingAndThenShowsTheEventsFromNewestToOldest() = runTest {
        val viewModel = viewModel()

        assertEquals(true, viewModel.state.value.loading)

        val state = viewModel.state.first { !it.loading }

        assertEquals(listOf(recent, middle, old), state.events)
        assertEquals(null, state.filter)
    }

    @Test
    fun filteringByTypeShowsOnlyThatTypeAndNullShowsAllAgain() = runTest {
        val viewModel = viewModel()
        viewModel.state.first { !it.loading }

        viewModel.onFilterChanged(EventType.Error)
        assertEquals(listOf(old), viewModel.state.first { it.filter == EventType.Error }.events)

        viewModel.onFilterChanged(null)
        assertEquals(listOf(recent, middle, old), viewModel.state.first { it.filter == null }.events)
    }

    @Test
    fun sendingByEmailSendsTheEventToTheDevelopers() = runTest {
        every { localFileHelper.sendFileByEmail(any(), any(), any(), any<String>()) } returns true

        viewModel().onSendByEmail(recent)

        verify {
            localFileHelper.sendFileByEmail(
                any(),
                "Informe de Evento Datwall",
                "Warning",
                "reciente \n mensaje reciente"
            )
        }
    }

    @Test
    fun withoutAnEmailAppTheScreenIsToldSo() = runTest {
        every { localFileHelper.sendFileByEmail(any(), any(), any(), any<String>()) } returns false
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onSendByEmail(recent)
            assertEquals(EventsEvent.NoEmailApp, awaitItem())
        }
    }
}
