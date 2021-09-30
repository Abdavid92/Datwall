package com.smartsolutions.paquetes.ui.settings

import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import kotlin.coroutines.CoroutineContext

class DataStorePreferences(
    private val dataStore: DataStore<Preferences>
) : SharedPreferences, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private val preferenceChangeListenerList = mutableListOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    private val mEditor = Editor()

    override fun getAll(): MutableMap<String, *> {
        return runBlocking {
            return@runBlocking dataStore.data
                .firstOrNull()
                ?.asMap()
                ?.map {
                    return@map it.key.name to it.value
                }?.toMap()
                ?.toMutableMap() ?: mutableMapOf()
        }
    }

    override fun getString(key: String?, default: String?): String? {
        return getValue(key, default)
    }

    override fun getStringSet(key: String?, default: MutableSet<String>?): MutableSet<String>? {
        return getValue(key, default)
    }

    override fun getInt(key: String?, default: Int): Int {
        return getValue(key, default)
    }

    override fun getLong(key: String?, default: Long): Long {
        return getValue(key, default)
    }

    override fun getFloat(key: String?, default: Float): Float {
        return getValue(key, default)
    }

    override fun getBoolean(key: String?, default: Boolean): Boolean {
        return getValue(key, default)
    }

    override fun contains(key: String?): Boolean {
        if (key != null)
            return PreferencesKeys.findPreferenceByKey<Any>(key) != null

        return false
    }

    override fun edit(): SharedPreferences.Editor {
        return this.mEditor
    }

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let {
            preferenceChangeListenerList.add(it)
        }
    }

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
        listener?.let {
            preferenceChangeListenerList.remove(it)
        }
    }

    private fun <T> getValue(key: String?, default: T): T {
        return runBlocking {
            if (key != null) {
                val dataKey = PreferencesKeys.findPreferenceByKey<T>(key)

                if (dataKey != null) {
                    return@runBlocking dataStore.data.firstOrNull()
                        ?.get(dataKey) ?: default
                } else
                    return@runBlocking default
            } else
                return@runBlocking default
        }
    }

    inner class Editor : SharedPreferences.Editor {

        private val pendingOperations = mutableListOf<suspend () -> String?>()

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            return put(key, value)
        }

        override fun putStringSet(key: String?, value: MutableSet<String>?): SharedPreferences.Editor {
            return put(key, value)
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            return put(key, value)
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            return put(key, value)
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            return put(key, value)
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            return put(key, value)
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            key?.let {
                PreferencesKeys.findPreferenceByKey<Any>(key)?.let { dataKey ->
                    val operation = suspend {
                        dataStore.edit {
                            it.remove(dataKey)
                        }

                        dataKey.name
                    }

                    pendingOperations.add(operation)
                }
            }

            return this
        }

        override fun clear(): SharedPreferences.Editor {
            val operation = suspend {
                dataStore.edit {
                    it.clear()
                }

                null
            }

            pendingOperations.add(operation)

            return this
        }

        override fun commit(): Boolean {
            return runBlocking {
                return@runBlocking try {
                    pendingOperations.forEach {

                        it()?.let { key ->
                            withContext(Dispatchers.Main) {

                                preferenceChangeListenerList.forEach {
                                    it.onSharedPreferenceChanged(this@DataStorePreferences, key)
                                }
                            }
                        }
                    }

                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        override fun apply() {
            launch {
                pendingOperations.forEach {
                    it()?.let { key ->
                        withContext(Dispatchers.Main) {

                            preferenceChangeListenerList.forEach {
                                it.onSharedPreferenceChanged(this@DataStorePreferences, key)
                            }
                        }
                    }
                }
            }
        }

        private fun <T> put(key: String?, value: T?): SharedPreferences.Editor {
            key?.let {
                PreferencesKeys.findPreferenceByKey<T>(key)?.let { dataKey ->
                    if (value != null) {
                        val operation = suspend {
                            dataStore.edit {
                                it[dataKey] = value
                            }

                            dataKey.name
                        }

                        pendingOperations.add(operation)
                    }
                }
            }

            return this
        }
    }
}