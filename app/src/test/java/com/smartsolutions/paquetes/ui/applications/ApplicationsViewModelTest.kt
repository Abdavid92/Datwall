package com.smartsolutions.paquetes.ui.applications

import com.smartsolutions.paquetes.MainDispatcherRule
import com.smartsolutions.paquetes.managers.AppListRules
import com.smartsolutions.paquetes.managers.contracts.IAppsManager
import com.smartsolutions.paquetes.managers.models.AppListSectionType
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import com.smartsolutions.paquetes.repositories.models.IApp
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ApplicationsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun app(name: String, access: Boolean = true, system: Boolean = false) = App().apply {
        packageName = "pkg.$name"
        uid = name.hashCode()
        this.name = name
        this.access = access
        this.system = system
    }

    // Como Room: emite cada lectura aunque sea "igual" por equals (un StateFlow la descartaría).
    private val appsFlow = MutableSharedFlow<List<IApp>>(replay = 1).apply {
        tryEmit(listOf(app("b"), app("a", access = false), app("sys", system = true)))
    }
    private val apps get() = appsFlow.replayCache.last()

    private val preferences = MutableStateFlow(AppPreferences(appsFilter = AppsFilter.InternetAccess.name))

    private val appsManager = mockk<IAppsManager>(relaxed = true) {
        every { flowApps() } returns appsFlow
        every { sections(any(), any()) } answers { AppListRules.sections(firstArg(), secondArg()) }
        every { search(any(), any()) } answers { AppListRules.search(firstArg(), secondArg()) }
    }
    private val preferencesRepository = mockk<IPreferencesRepository>(relaxed = true) {
        every { this@mockk.preferences } returns this@ApplicationsViewModelTest.preferences
    }

    private fun viewModel() = ApplicationsViewModel(appsManager, preferencesRepository, mockk())

    @Test
    fun initialStateHasTheSectionsOfEachTab() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(false, state.loading)
        assertEquals(AppsFilter.InternetAccess, state.filter)
        assertEquals(
            listOf(AppListSectionType.ALLOWED, AppListSectionType.BLOCKED),
            state.userSections.map { it.type }
        )
        assertEquals(listOf("sys"), state.systemSections.flatMap { it.apps }.map { it.name })
    }

    /**
     * `App.equals` ignora el acceso: un cambio de acceso no debe perderse por parecer "el mismo
     * estado".
     */
    @Test
    fun accessChangesComingFromTheDatabaseReachTheState() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        assertEquals(listOf("b"), viewModel.state.value.userSections.first().apps.map { it.name })

        appsFlow.tryEmit(listOf(app("b", access = false), app("a", access = false), app("sys", system = true)))
        advanceUntilIdle()

        val sections = viewModel.state.value.userSections
        assertEquals(listOf(AppListSectionType.BLOCKED), sections.map { it.type })
    }

    @Test
    fun searchShowsOnlyTheMatchesWithoutHeaders() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onQueryChanged("b")
        advanceUntilIdle()

        val sections = viewModel.state.value.userSections
        assertEquals(listOf(null), sections.map { it.type })
        assertEquals(listOf("b"), sections.single().apps.map { it.name })
    }

    @Test
    fun accessChangesAreSavedOnCommit() = runTest {
        val viewModel = viewModel()
        val changed = apps[0]

        assertEquals(false, viewModel.onCommitChanges())

        viewModel.onAccessChanged(changed)
        assertEquals(true, viewModel.onCommitChanges())
        advanceUntilIdle()

        coVerify { appsManager.commit(listOf(changed)) }
    }

    @Test
    fun changingTheFilterIsRemembered() = runTest {
        val viewModel = viewModel()

        viewModel.onFilterChanged(AppsFilter.Alphabetic)
        advanceUntilIdle()

        coVerify { preferencesRepository.setAppsFilter("Alphabetic") }
    }
}
