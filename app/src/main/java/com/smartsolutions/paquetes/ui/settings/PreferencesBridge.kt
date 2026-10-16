package com.smartsolutions.paquetes.ui.settings

import androidx.preference.PreferenceDataStore
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.repositories.contracts.IPreferencesRepository
import com.smartsolutions.paquetes.repositories.contracts.PreferencesStore

/**
 * Puente entre las pantallas de preferencias y [IPreferencesRepository]: lee de la copia en
 * memoria (sin bloquear) y guarda sin esperar.
 * */
class PreferencesBridge(
    private val repository: IPreferencesRepository,
    private val store: PreferencesStore
) : PreferenceDataStore() {

    override fun putString(key: String, value: String?) = putValue(key, value)

    override fun putStringSet(key: String, values: MutableSet<String>?) = putValue(key, values)

    override fun putInt(key: String, value: Int) = putValue(key, value)

    override fun putLong(key: String, value: Long) = putValue(key, value)

    override fun putFloat(key: String, value: Float) = putValue(key, value)

    override fun putBoolean(key: String, value: Boolean) = putValue(key, value)

    override fun getString(key: String?, defValue: String?): String? =
        key?.let { getValue<String>(it) } ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> =
        key?.let { getValue<Set<String>>(it)?.toMutableSet() } ?: defValues ?: mutableSetOf()

    override fun getInt(key: String?, defValue: Int): Int = key?.let { getValue<Int>(it) } ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = key?.let { getValue<Long>(it) } ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = key?.let { getValue<Float>(it) } ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        key?.let { getValue<Boolean>(it) } ?: defValue

    private fun <T> putValue(key: String, value: T?) {
        value ?: return
        PreferencesKeys.findPreferenceByKey<T>(key)?.let { preferenceKey ->
            repository.putLater(store, preferenceKey, value)
        }
    }

    private fun <T> getValue(key: String): T? =
        PreferencesKeys.findPreferenceByKey<T>(key)?.let { repository.snapshot(store).value[it] }
}
