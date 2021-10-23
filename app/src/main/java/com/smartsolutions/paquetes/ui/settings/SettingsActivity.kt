package com.smartsolutions.paquetes.ui.settings

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.annotation.Keep
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.preference.*
import com.smartsolutions.paquetes.*
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentAboutBinding
import com.smartsolutions.paquetes.databinding.FragmentNotificationStyleBinding
import com.smartsolutions.paquetes.databinding.FragmentThemesBinding
import com.smartsolutions.paquetes.databinding.ItemNotificationSampleBinding
import com.smartsolutions.paquetes.helpers.NotificationHelper
import com.smartsolutions.paquetes.helpers.uiHelper
import com.smartsolutions.paquetes.services.CircularNotificationBuilder
import com.smartsolutions.paquetes.services.LinearNotificationBuilder
import com.smartsolutions.paquetes.services.NotificationBuilder
import com.smartsolutions.paquetes.ui.AbstractActivity
import com.smartsolutions.paquetes.ui.SplashActivity
import com.smartsolutions.paquetes.ui.activation.ActivationActivity
import com.smartsolutions.paquetes.ui.events.EventsFragment
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

    @Keep
    class ThemesFragment : Fragment(), CoroutineScope, TitleFragment {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main

        private lateinit var binding: FragmentThemesBinding

        private lateinit var mTitle: CharSequence

        private val uiHelper by uiHelper()

        private val preferenceDataStore by lazy {
            AbstractPreferenceFragmentCompat.PreferenceDataStore(requireContext().settingsDataStore)
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            setHasOptionsMenu(true)

            mTitle = arguments?.getCharSequence(TITLE_TAG) ?: "Themes"
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
                val dataStore = requireContext().settingsDataStore

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

                        //TODO Pendiente a aprobación este método
                        activity?.recreate()
                        //showRestartDialog()
                    }
                }
            }
        }

        override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
            super.onCreateOptionsMenu(menu, inflater)

            inflater.inflate(R.menu.themes_menu, menu)
        }

        override fun onOptionsItemSelected(item: MenuItem): Boolean {

            when (item.itemId) {
                R.id.action_random_theme -> {
                    val themes = uiHelper.getThemeList()

                    val r = Random(System.currentTimeMillis())
                        .nextInt(themes.size)

                    val theme = themes[r]

                    preferenceDataStore.putInt(PreferencesKeys.APP_THEME.name, theme.id)

                    showRestartDialog()
                }
                R.id.action_restore_default -> {
                    preferenceDataStore.putInt(
                            PreferencesKeys.THEME_MODE.name,
                            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        )

                    preferenceDataStore
                        .putInt(PreferencesKeys.APP_THEME.name, R.style.Theme_Datwall)

                    AppCompatDelegate
                        .setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

                    requireActivity().recreate()
                }
            }

            return super.onOptionsItemSelected(item)
        }

        override fun title(): CharSequence {
            return mTitle
        }
    }

    @Keep
    class SynchronizationFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = PreferenceDataStore(requireContext().workersDataStore)
            setPreferencesFromResource(R.xml.sync_preferences, rootKey)
        }
    }

    @Keep
    class NotificationsFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.notifications_preferences, rootKey)
        }
    }

    @Keep
    class NotificationStyleFragment : Fragment(),
        CoroutineScope, TitleFragment {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.IO

        private lateinit var mTitle: CharSequence

        private lateinit var binding: FragmentNotificationStyleBinding

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            mTitle = arguments?.getCharSequence(TITLE_TAG) ?: "Notification style"
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentNotificationStyleBinding.inflate(
                inflater,
                container,
                false
            )

            val circularNotification = CircularNotificationBuilder(
                requireContext(),
                NotificationHelper.ALERT_CHANNEL_ID
            )

            val circularLayout = ItemNotificationSampleBinding.inflate(
                inflater,
                container,
                false
            )
            circularLayout.sampleContainer.addView(circularNotification.getSample(container))
            circularLayout.summary.text = circularNotification.getSummary()
            binding.circularNotificationSample.addView(circularLayout.root)

            val linearNotification = LinearNotificationBuilder(
                requireContext(),
                NotificationHelper.ALERT_CHANNEL_ID
            )

            val linearLayout = ItemNotificationSampleBinding.inflate(
                inflater,
                container,
                false
            )

            linearLayout.sampleContainer.addView(linearNotification.getSample(container))
            linearLayout.summary.text = linearNotification.getSummary()
            binding.linealNotificationSample.addView(linearLayout.root)

            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            runBlocking {
                val dataStore = requireContext().settingsDataStore

                var className = dataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.NOTIFICATION_CLASS) ?:
                    NotificationBuilder.DEFAULT_NOTIFICATION_IMPL

                when (className) {
                    CircularNotificationBuilder::class.java.canonicalName -> {
                        binding.radioGroup.check(R.id.circular_notification)
                    }
                    LinearNotificationBuilder::class.java.canonicalName -> {
                        binding.radioGroup.check(R.id.linear_notification)
                    }
                }

                binding.radioGroup.setOnCheckedChangeListener { _, i ->

                    when (i) {
                        R.id.circular_notification -> {
                            className = CircularNotificationBuilder::class.java.name
                        }
                        R.id.linear_notification -> {
                            className = LinearNotificationBuilder::class.java.name
                        }
                    }

                    runBlocking(Dispatchers.IO) {
                        requireContext().settingsDataStore.edit {
                            it[PreferencesKeys.NOTIFICATION_CLASS] = className
                        }
                    }
                }
            }
        }

        override fun title(): CharSequence {
            return mTitle
        }
    }

    @Keep
    @AndroidEntryPoint
    class HistoryFragment : AbstractPreferenceFragmentCompat() {

        private val viewModel by viewModels<SettingsViewModel>()

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.history_preferences, rootKey)

            findPreference<Preference>("clear_history")
                ?.setOnPreferenceClickListener {

                    AlertDialog.Builder(requireContext())
                        .setTitle(R.string.clear_history)
                        .setMessage(R.string.clear_history_confirmation)
                        .setNegativeButton(R.string.btn_not, null)
                        .setPositiveButton(R.string.btn_yes) { _,_ ->
                            viewModel.clearHistory()
                        }.show()

                    return@setOnPreferenceClickListener true
                }

            findPreference<Preference>("clear_icon_cache")
                ?.setOnPreferenceClickListener {

                    viewModel.clearIconCache()

                    Toast.makeText(
                        requireContext(),
                        R.string.clear_icon_cache_confirmation,
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnPreferenceClickListener true
                }

            findPreference<Preference>("show_events")
                ?.setOnPreferenceClickListener {
                    val fragment = EventsFragment.newInstance()
                    fragment.show(childFragmentManager, null)

                    return@setOnPreferenceClickListener true
                }
        }
    }

    @Keep
    class UpdatesFragment : AbstractPreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            super.onCreatePreferences(savedInstanceState, rootKey)
            setPreferencesFromResource(R.xml.updates_preferences, rootKey)
        }
    }

    @Keep
    class AboutFragment : Fragment(), TitleFragment {

        private lateinit var binding: FragmentAboutBinding

        private lateinit var mTitle: String

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)

            mTitle = arguments?.getString(TITLE_TAG) ?: "About..."
        }

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentAboutBinding.inflate(
                inflater,
                container,
                false
            )

            return binding.root
        }

        @SuppressLint("SetTextI18n")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            binding.appVersion.text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

            binding.sendEmail.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.DEVELOPERS_EMAIL))
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                startActivity(intent)
            }

            binding.telegramChannel.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(BuildConfig.TELEGRAM_CHANNEL))

                startActivity(intent)
            }

            binding.apklisStore.setOnClickListener {
                val intent = Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse(BuildConfig.APKLIS_URL))

                startActivity(intent)
            }
        }

        override fun title(): CharSequence {
            return mTitle
        }
    }

    abstract class AbstractPreferenceFragmentCompat : PreferenceFragmentCompat(),
        TitleFragment,
        CoroutineScope {

        override val coroutineContext: CoroutineContext
            get() = Dispatchers.Main

        private lateinit var mTitle: CharSequence

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            mTitle = arguments?.getCharSequence(TITLE_TAG) ?: "SettingsFragment"
        }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            preferenceManager.preferenceDataStore = PreferenceDataStore(requireContext().settingsDataStore)
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
                putValue(key, value)
            }

            override fun putStringSet(key: String, values: MutableSet<String>?) {
                putValue(key, values)
            }

            override fun putInt(key: String, value: Int) {
                putValue(key, value)
            }

            override fun putLong(key: String, value: Long) {
                putValue(key, value)
            }

            override fun putFloat(key: String, value: Float) {
                putValue(key, value)
            }

            override fun putBoolean(key: String, value: Boolean) {
                putValue(key, value)
            }

            override fun getString(key: String?, defValue: String?): String? {
                key?.let {
                    return getValue(it)
                }
                return defValue
            }

            override fun getStringSet(
                key: String?,
                defValues: MutableSet<String>?
            ): MutableSet<String> {
                key?.let {
                    return getValue(it) ?: mutableSetOf()
                }

                return defValues ?: mutableSetOf()
            }

            override fun getInt(key: String?, defValue: Int): Int {
                key?.let {
                    return getValue(it) ?: defValue
                }

                return defValue
            }

            override fun getLong(key: String?, defValue: Long): Long {
                key?.let {
                    return getValue(it) ?: defValue
                }

                return defValue
            }

            override fun getFloat(key: String?, defValue: Float): Float {
                key?.let {
                    return getValue(it) ?: defValue
                }

                return defValue
            }

            override fun getBoolean(key: String?, defValue: Boolean): Boolean {
                key?.let {
                    return getValue(it) ?: defValue
                }

                return defValue
            }

            private fun <T> putValue(key: String, value: T?) {
                value?.let { v ->
                    PreferencesKeys.findPreferenceByKey<T>(key)?.let { preferenceKey ->
                        launch {
                            dataStore.edit {
                                it[preferenceKey] = v
                            }
                        }
                    }
                }
            }

            private fun <T> getValue(key: String): T? {
                PreferencesKeys.findPreferenceByKey<T>(key)?.let { dataKey ->
                    return runBlocking(Dispatchers.IO) {
                        return@runBlocking dataStore.data.firstOrNull()
                            ?.get(dataKey)
                    }
                }

                return null
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