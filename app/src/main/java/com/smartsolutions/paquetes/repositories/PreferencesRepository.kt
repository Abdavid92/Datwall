package com.smartsolutions.paquetes.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.contracts.PreferencesStore
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lee y escribe las mismas claves y DataStore que usaba cada pantalla, sin cambiar nombres
 * ni valores.
 *
 * @param settings DataStore "settings".
 * @param internal DataStore "internal_settings".
 * @param ui DataStore "settings_ui".
 * @param workers DataStore "workers_settings".
 * */
class PreferencesRepository(
    private val settings: DataStore<Preferences>,
    private val internal: DataStore<Preferences>,
    private val ui: DataStore<Preferences>,
    private val workers: DataStore<Preferences>,
    private val scope: CoroutineScope
) : IPreferencesRepository {

    private val settingsSnapshot = settings.data.stateIn(scope, SharingStarted.Eagerly, emptyPreferences())
    private val workersSnapshot = workers.data.stateIn(scope, SharingStarted.Eagerly, emptyPreferences())

    private val loaded = combine(settings.data, internal.data, ui.data) { s, i, u ->
        AppPreferences(
            themeMode = s[PreferencesKeys.THEME_MODE],
            appTheme = s[PreferencesKeys.APP_THEME],
            notificationClass = s[PreferencesKeys.NOTIFICATION_CLASS],
            foregroundAppModern = s[PreferencesKeys.IS_FOREGROUND_APP_MODERN],
            appsFilter = s[PreferencesKeys.APPS_FILTER],
            usagePeriod = u[PreferencesKeys.USAGE_PERIOD],
            usageFilter = u[PreferencesKeys.USAGE_FILTER],
            resumeFilter = u[PreferencesKeys.RESUME_FILTER],
            bubbleSize = u[PreferencesKeys.BUBBLE_SIZE],
            bubbleTransparency = u[PreferencesKeys.BUBBLE_TRANSPARENCY],
            bubbleAlwaysShow = u[PreferencesKeys.BUBBLE_ALWAYS_SHOW],
            dynamicFirewall = i[PreferencesKeys.ENABLED_DYNAMIC_FIREWALL],
            dataDisclosureAnswered = s[PreferencesKeys.DATA_DISCLOSURE_ANSWERED],
            simSetupSkipped = s[PreferencesKeys.SIM_SETUP_SKIPPED]
        )
    }.shareIn(scope, SharingStarted.Eagerly, replay = 1)

    override val preferences: StateFlow<AppPreferences> =
        loaded.stateIn(scope, SharingStarted.Eagerly, AppPreferences())

    override suspend fun awaitLoaded(): AppPreferences = loaded.first()

    override suspend fun setThemeMode(mode: Int) = set(settings, PreferencesKeys.THEME_MODE, mode)

    override suspend fun setAppTheme(theme: Int) = set(settings, PreferencesKeys.APP_THEME, theme)

    override suspend fun setNotificationClass(className: String) =
        set(settings, PreferencesKeys.NOTIFICATION_CLASS, className)

    override suspend fun setForegroundAppModern(modern: Boolean) =
        set(settings, PreferencesKeys.IS_FOREGROUND_APP_MODERN, modern)

    override suspend fun setAppsFilter(filter: String) = set(settings, PreferencesKeys.APPS_FILTER, filter)

    override suspend fun setUsagePeriod(period: Int) = set(ui, PreferencesKeys.USAGE_PERIOD, period)

    override suspend fun setUsageFilter(filter: String) = set(ui, PreferencesKeys.USAGE_FILTER, filter)

    override suspend fun setResumeFilter(filter: String) = set(ui, PreferencesKeys.RESUME_FILTER, filter)

    override suspend fun setBubbleSize(size: String) = set(ui, PreferencesKeys.BUBBLE_SIZE, size)

    override suspend fun setBubbleTransparency(transparency: Float) =
        set(ui, PreferencesKeys.BUBBLE_TRANSPARENCY, transparency)

    override suspend fun setBubbleAlwaysShow(alwaysShow: Boolean) =
        set(ui, PreferencesKeys.BUBBLE_ALWAYS_SHOW, alwaysShow)

    override suspend fun setDynamicFirewall(enabled: Boolean) =
        set(internal, PreferencesKeys.ENABLED_DYNAMIC_FIREWALL, enabled)

    override suspend fun setDataDisclosureAnswered(answered: Boolean) =
        set(settings, PreferencesKeys.DATA_DISCLOSURE_ANSWERED, answered)

    override suspend fun setSimSetupSkipped(skipped: Boolean) =
        set(settings, PreferencesKeys.SIM_SETUP_SKIPPED, skipped)

    override fun snapshot(store: PreferencesStore): StateFlow<Preferences> = when (store) {
        PreferencesStore.SETTINGS -> settingsSnapshot
        PreferencesStore.WORKERS -> workersSnapshot
    }

    override fun <T> putLater(store: PreferencesStore, key: Preferences.Key<T>, value: T) {
        scope.launch {
            set(
                when (store) {
                    PreferencesStore.SETTINGS -> settings
                    PreferencesStore.WORKERS -> workers
                },
                key,
                value
            )
        }
    }

    private suspend fun <T> set(dataStore: DataStore<Preferences>, key: Preferences.Key<T>, value: T) {
        dataStore.edit { it[key] = value }
    }
}
