package com.smartsolutions.paquetes.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.preference.PreferenceDataStore
import androidx.test.platform.app.InstrumentationRegistry
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.settingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before

import org.junit.Test

class PreferenceDataStoreTest {

    private lateinit var preferenceDataStore: PreferenceDataStore

    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation()
            .targetContext

        dataStore = context.settingsDataStore

        preferenceDataStore = SettingsActivity.AbstractPreferenceFragmentCompat.PreferenceDataStore(dataStore)
    }

    @Test
    fun putBoolean() {

        runBlocking {
            preferenceDataStore.putBoolean("enabled_firewall", true)
        }
    }

    @Test
    fun getBoolean() {
        runBlocking {

            val enabledFirewall = dataStore.data
                .firstOrNull()?.get(PreferencesKeys.ENABLED_FIREWALL) == true


            assertTrue(enabledFirewall)
        }
    }
}