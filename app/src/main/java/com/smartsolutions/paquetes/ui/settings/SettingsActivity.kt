package com.smartsolutions.paquetes.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.preference.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.databinding.FragmentThemesBinding
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.activation.ActivationActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

private const val TITLE_TAG = "settingsActivityTitle"

@AndroidEntryPoint
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
            } else {
                val fragments = supportFragmentManager
                    .fragments

                val fragment = fragments[fragments.size - 1]

                if (fragment is TitleFragment) {
                    title = fragment.title()
                }
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
        val args = pref.extras.apply {
            putCharSequence(TITLE_TAG, pref.title)
        }
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()

        return true
    }

    class HeaderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.header_preferences, rootKey)

            findPreference<Preference>("activation_key")
                ?.setOnPreferenceClickListener {

                    startActivity(Intent(requireContext(), ActivationActivity::class.java))

                    return@setOnPreferenceClickListener true
                }
        }
    }

    class UIFragment : AbstractPreferenceFragmentCompat() {

        private val uiHelper by uiHelper()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.appaerance_preferences, rootKey)

            findPreference<Preference>("random_theme")
                ?.setOnPreferenceClickListener {

                    val themes = uiHelper.getThemeList()

                    val r = Random(System.currentTimeMillis())
                        .nextInt(themes.size)

                    val theme = themes[r]

                    preferenceManager.preferenceDataStore
                        ?.putInt(PreferencesKeys.APP_THEME.name, theme.id)

                    showRestartDialog()

                    return@setOnPreferenceClickListener true
            }

            findPreference<Preference>("restore_default")
                ?.setOnPreferenceClickListener {

                    preferenceManager.preferenceDataStore
                        ?.putInt(
                            PreferencesKeys.THEME_MODE.name,
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        )

                    preferenceManager.preferenceDataStore
                        ?.putInt(PreferencesKeys.APP_THEME.name, R.style.Theme_Datwall)

                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

                    showRestartDialog()

                    return@setOnPreferenceClickListener true
                }
        }
    }

    class ThemesFragment : Fragment(), CoroutineScope, TitleFragment {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main

        private lateinit var binding: FragmentThemesBinding

        private lateinit var mTitle: CharSequence

        private val uiHelper by uiHelper()

        private val preferenceDataStore by lazy {
            AbstractPreferenceFragmentCompat.PreferenceDataStore(requireContext().dataStore)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            mTitle = arguments?.getCharSequence(TITLE_TAG) ?: "SettingsFragment"
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentThemesBinding.inflate(
                inflater,
                container,
                false
            )

            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            launch {
                val dataStore = requireContext().dataStore

                var themeMode = dataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.THEME_MODE) ?: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

                when (themeMode) {
                    AppCompatDelegate.MODE_NIGHT_NO -> {
                        binding.themeLight.isChecked = true
                    }
                    AppCompatDelegate.MODE_NIGHT_YES -> {
                        binding.themeDark.isChecked = true
                    }
                    AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> {
                        binding.themeSystem.isChecked = true
                    }
                }

                binding.radioButtons.setOnCheckedChangeListener { _, id ->

                    themeMode = when (id) {
                        R.id.theme_light -> {
                            AppCompatDelegate.MODE_NIGHT_NO
                        }
                        R.id.theme_dark -> {
                            AppCompatDelegate.MODE_NIGHT_YES
                        }
                        R.id.theme_system -> {
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        }
                        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                    }

                    preferenceDataStore.putInt(
                        PreferencesKeys.THEME_MODE.name,
                        themeMode
                    )

                    AppCompatDelegate.setDefaultNightMode(themeMode)
                }

                val currentTheme = dataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.APP_THEME) ?: R.style.Theme_Datwall

                binding.themeList.adapter = ThemesAdapter(
                    uiHelper,
                    currentTheme
                ).apply {
                    onThemeChange = { newTheme ->

                        preferenceDataStore.putInt(
                            PreferencesKeys.APP_THEME.name,
                            newTheme
                        )

                        showRestartDialog()
                    }
                }
            }
        }

        override fun title(): CharSequence {
            return mTitle
        }
    }

    class NotificationsFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.notifications_preferences, rootKey)
        }
    }

    class HistoryFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.history_preferences, rootKey)
        }
    }

    class UpdatesFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.updates_preferences, rootKey)
        }
    }

    class AboutFragment : Fragment() {

    }

    abstract class AbstractPreferenceFragmentCompat : PreferenceFragmentCompat(), TitleFragment {

        private lateinit var mTitle: CharSequence

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            preferenceManager.preferenceDataStore = PreferenceDataStore(requireContext().dataStore)
            mTitle = arguments?.getCharSequence(TITLE_TAG) ?: "SettingsFragment"
        }

        override fun title(): CharSequence {
            return mTitle
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

    interface TitleFragment {
        fun title(): CharSequence
    }
}

fun Fragment.showRestartDialog() {
    AlertDialog.Builder(requireContext())
        .setTitle(R.string.need_restart)
        .setMessage(R.string.need_restart_summary)
        .setNegativeButton(R.string.btn_cancel, null)
        .setPositiveButton(R.string.restart
        ) { _,_ ->
            startActivity(Intent(requireContext(), SplashActivity::class.java))
            requireActivity().finishAffinity()
        }.show()
}