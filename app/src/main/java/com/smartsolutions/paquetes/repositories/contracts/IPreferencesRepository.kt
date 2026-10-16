package com.smartsolutions.paquetes.repositories.contracts

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.smartsolutions.paquetes.repositories.models.AppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.StateFlow

/**
 * Acceso común a las preferencias de la UI. Mantiene una copia en memoria para leer sin
 * bloquear el hilo de la interfaz.
 * */
interface IPreferencesRepository {

    /**
     * Copia en memoria; se actualiza con cada cambio.
     * */
    val preferences: StateFlow<AppPreferences>

    /**
     * Espera a la primera lectura de disco y la devuelve.
     * */
    suspend fun awaitLoaded(): AppPreferences

    suspend fun setThemeMode(mode: Int)
    suspend fun setAppTheme(theme: Int)
    suspend fun setNotificationClass(className: String)
    suspend fun setForegroundAppModern(modern: Boolean)
    suspend fun setAppsFilter(filter: String)
    suspend fun setUsagePeriod(period: Int)
    suspend fun setUsageFilter(filter: String)
    suspend fun setResumeFilter(filter: String)
    suspend fun setBubbleSize(size: String)
    suspend fun setBubbleTransparency(transparency: Float)
    suspend fun setBubbleAlwaysShow(alwaysShow: Boolean)
    suspend fun setDynamicFirewall(enabled: Boolean)
    suspend fun setDataDisclosureAnswered(answered: Boolean)
    suspend fun setSimSetupSkipped(skipped: Boolean)

    /**
     * Copia en memoria de todas las claves de [store], para las pantallas de ajustes que leen
     * cualquier clave por su nombre.
     * */
    fun snapshot(store: PreferencesStore): StateFlow<Preferences>

    /**
     * Guarda [value] en [store] sin esperar; el guardado sigue aunque se cierre la pantalla.
     * */
    fun <T> putLater(store: PreferencesStore, key: Preferences.Key<T>, value: T)
}

/**
 * DataStore que usan las pantallas de ajustes.
 * */
enum class PreferencesStore {
    /**
     * "settings".
     * */
    SETTINGS,

    /**
     * "workers_settings" (sincronización).
     * */
    WORKERS
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PreferencesRepositoryEntryPoint {
    fun preferencesRepository(): IPreferencesRepository
}

/**
 * Preferencias para clases que no se pueden inyectar (fragmentos de ajustes).
 * */
fun Context.preferencesRepository(): IPreferencesRepository =
    EntryPointAccessors.fromApplication(applicationContext, PreferencesRepositoryEntryPoint::class.java)
        .preferencesRepository()
