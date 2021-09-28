package com.smartsolutions.paquetes.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.coroutines.CoroutineContext

private const val TITLE_TAG = "settingsActivityTitle"

class SettingsActivity : AbstractActivity(R.layout.activity_settings),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setSupportActionBar(findViewById(R.id.toolbar))

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, HeaderFragment())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }

        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.title_activity_settings)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current activity title so we can set it again after a configuration change
        outState.putCharSequence(TITLE_TAG, title)
    }

    override fun onSupportNavigateUp(): Boolean {
        if (supportFragmentManager.popBackStackImmediate()) {
            return true
        }
        return super.onSupportNavigateUp()
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
            //setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey)
        }
    }

    class UIFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.appaerance_preferences, rootKey)

            findPreference<Preference>("random_theme")
                ?.setOnPreferenceClickListener {

                    return@setOnPreferenceClickListener true
            }

            findPreference<Preference>("restore_default")
                ?.setOnPreferenceClickListener {

                    preferenceManager.preferenceDataStore
                        ?.putString(PreferencesKeys.THEME_MODE.name, "system")

                    preferenceManager.preferenceDataStore
                        ?.putInt(PreferencesKeys.APP_THEME.name, R.style.Theme_Datwall_Blue)

                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.need_restart)
                        .setMessage(R.string.need_restart_summary)
                        .setNegativeButton(R.string.btn_cancel, null)
                        .setPositiveButton(R.string.restart
                        ) { _,_ ->
                            requireActivity().recreate()
                        }.show()

                    return@setOnPreferenceClickListener true
                }
        }
    }

    class ThemesFragment : Fragment() {

    }

    class MessagesFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.messages_preferences, rootKey)
        }
    }

    class SyncFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.sync_preferences, rootKey)
        }
    }

    abstract class AbstractPreferenceFragmentCompat : PreferenceFragmentCompat() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.preferenceDataStore = PreferenceDataStore(requireContext().dataStore)
        }

        class PreferenceDataStore(
            private val dataStore: DataStore<Preferences>
        ) : androidx.preference.PreferenceDataStore(), CoroutineScope {

            override val coroutineContext: CoroutineContext
                get() = Dispatchers.IO

            override fun putString(key: String, value: String?) {
                value?.let { v ->
                    PreferencesKeys.findPreferenceByKey<String>(key)?.let { preferenceKey ->
                        launch {
                            dataStore.edit {
                                it[preferenceKey] = v
                            }
                        }
                    }
                }
            }

            override fun putStringSet(key: String, values: MutableSet<String>?) {
                values?.let { v ->
                    PreferencesKeys.findPreferenceByKey<Set<String>>(key)?.let { preferenceKey ->
                        launch {
                            dataStore.edit {
                                it[preferenceKey] = v
                            }
                        }
                    }
                }
            }

            override fun putInt(key: String, value: Int) {
                PreferencesKeys.findPreferenceByKey<Int>(key)?.let { preferenceKey ->
                    launch {
                        dataStore.edit {
                            it[preferenceKey] = value
                        }
                    }
                }
            }

            override fun putLong(key: String, value: Long) {
                PreferencesKeys.findPreferenceByKey<Long>(key)?.let { preferenceKey ->
                    launch {
                        dataStore.edit {
                            it[preferenceKey] = value
                        }
                    }
                }
            }

            override fun putFloat(key: String, value: Float) {
                PreferencesKeys.findPreferenceByKey<Float>(key)?.let { preferenceKey ->
                    launch {
                        dataStore.edit {
                            it[preferenceKey] = value
                        }
                    }
                }
            }

            override fun putBoolean(key: String, value: Boolean) {
                PreferencesKeys.findPreferenceByKey<Boolean>(key)?.let { preferenceKey ->
                    launch {
                        dataStore.edit {
                            it[preferenceKey] = value
                        }
                    }
                }
            }
        }
    }
}