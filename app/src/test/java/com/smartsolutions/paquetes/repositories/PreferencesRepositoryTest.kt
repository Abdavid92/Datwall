package com.smartsolutions.paquetes.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.smartsolutions.paquetes.PreferencesKeys
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PreferencesRepositoryTest {

    /**
     * DataStore en memoria: el de archivos no se puede usar en Windows dentro de las pruebas
     * (no puede renombrar el archivo temporal mientras otro lector lo tiene abierto).
     * */
    private class InMemoryDataStore : DataStore<Preferences> {
        private val state = MutableStateFlow(emptyPreferences())

        override val data: StateFlow<Preferences> = state

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            transform(state.value).also { state.value = it }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun dataStore(scope: CoroutineScope, name: String): DataStore<Preferences> = InMemoryDataStore()

    private fun TestScope.repository(
        settings: DataStore<Preferences> = dataStore(backgroundScope, "settings"),
        internal: DataStore<Preferences> = dataStore(backgroundScope, "internal"),
        ui: DataStore<Preferences> = dataStore(backgroundScope, "ui")
    ) = PreferencesRepository(settings, internal, ui, InMemoryDataStore(), backgroundScope)

    @Test
    fun unsetPreferencesAreNull() = runTest(UnconfinedTestDispatcher()) {
        val preferences = repository().awaitLoaded()

        assertNull(preferences.themeMode)
        assertNull(preferences.usagePeriod)
        assertNull(preferences.bubbleTransparency)
        assertNull(preferences.dynamicFirewall)
    }

    @Test
    fun readsTheExistingKeysOfEachDataStore() = runTest(UnconfinedTestDispatcher()) {
        val settings = dataStore(backgroundScope, "settings")
        val internal = dataStore(backgroundScope, "internal")
        val ui = dataStore(backgroundScope, "ui")

        settings.edit {
            it[PreferencesKeys.THEME_MODE] = 2
            it[PreferencesKeys.APPS_FILTER] = "Alphabetic"
        }
        internal.edit { it[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL] = true }
        ui.edit {
            it[PreferencesKeys.USAGE_PERIOD] = 3
            it[PreferencesKeys.BUBBLE_TRANSPARENCY] = 0.5f
        }

        val preferences = repository(settings, internal, ui).awaitLoaded()

        assertEquals(2, preferences.themeMode)
        assertEquals("Alphabetic", preferences.appsFilter)
        assertEquals(true, preferences.dynamicFirewall)
        assertEquals(3, preferences.usagePeriod)
        assertEquals(0.5f, preferences.bubbleTransparency)
    }

    @Test
    fun writesGoToTheSameKeysAndUpdateTheCopy() = runTest(UnconfinedTestDispatcher()) {
        val ui = dataStore(backgroundScope, "ui")
        val repository = repository(ui = ui)
        repository.awaitLoaded()

        repository.setUsagePeriod(4)
        repository.setResumeFilter("Rest")

        assertEquals(4, ui.data.first()[PreferencesKeys.USAGE_PERIOD])
        assertEquals("Rest", ui.data.first()[PreferencesKeys.RESUME_FILTER])
        assertEquals(4, repository.preferences.value.usagePeriod)
        assertEquals("Rest", repository.preferences.value.resumeFilter)
    }
}
