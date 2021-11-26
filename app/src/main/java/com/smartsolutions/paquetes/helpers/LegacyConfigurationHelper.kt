package com.smartsolutions.paquetes.helpers

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import com.smartsolutions.paquetes.settingsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Obtiene las configuraciones de la versión anterior de la aplicación.
 * */
class LegacyConfigurationHelper @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val appRepository: IAppRepository
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    private lateinit var preferences: SharedPreferences

    private val dbList = mapOf<String, suspend () -> Unit>(
        "rules.db" to ::restoreRulesDb,
        "data.db" to ::restoreDataDb
    )

    init {
        launch {
            preferences = context
                .getSharedPreferences("data_mis_datos", Context.MODE_PRIVATE)
        }
    }

    /**
     * Indica si ya la versión anterior de la aplicación ha sido comprada.
     * */
    suspend fun isPurchased(): Boolean {
        return withContext(Dispatchers.IO) {
            preferences.getBoolean("l_p_f", false)
        }
    }

    suspend fun restoreOldRules() {

        val databaseList = context.databaseList()

        dbList.forEach {

            if (databaseList.contains(it.key))
                it.value()
        }
    }

    private suspend fun restoreRulesDb() {
        val db = context.openOrCreateDatabase("rules.db", Context.MODE_PRIVATE, null)

        val result = mutableListOf<String>()

        runCatching {

            val cursor = db.query(
                "apps",
                arrayOf("package_name"),
                "data_access = ?",
                arrayOf("1"),
                null,
                null,
                null
            )

            if (cursor.moveToFirst()) {
                var packageName = cursor.getString(cursor.getColumnIndex("package_name"))

                result.add(packageName)

                while (cursor.moveToNext()) {
                    packageName = cursor.getString(cursor.getColumnIndex("package_name"))

                    result.add(packageName)
                }
            }
            cursor.close()

            context.deleteDatabase("rules.db")
        }

        val apps = appRepository.all().filter { !it.access }

        val updateApps = mutableListOf<App>()

        result.forEach { packageName ->

            apps.firstOrNull { a -> a.packageName == packageName }?.let {
                updateApps.add(it.apply {
                    access = true
                })
            }
        }

        if (updateApps.isNotEmpty()) {
            appRepository.update(updateApps)
        }
    }

    private suspend fun restoreDataDb() {

        val db = context.openOrCreateDatabase("data.db", Context.MODE_PRIVATE, null)

        val result = mutableListOf<App>()

        kotlin.runCatching {
            val cursor = db.query(
                "apps",
                null,
                null,
                null,
                null,
                null,
                null
            )

            if (cursor.moveToFirst()) {

                result.add(fillApp(cursor))

                while (cursor.moveToNext()) {
                    result.add(fillApp(cursor))
                }
            }

            cursor.close()

            context.deleteDatabase("data.db")
        }

        val apps = appRepository.all()
        val appsUpdate = mutableListOf<App>()

        result.forEach { appResult ->
            apps.firstOrNull { it == appResult }?.let {
                appsUpdate.add(appResult)
            }
        }

        if (appsUpdate.isNotEmpty())
            appRepository.update(appsUpdate)
    }


    private fun fillApp(cursor: Cursor): App {
        return App(
            cursor.getString(cursor.getColumnIndex("package_name")),
            cursor.getInt(cursor.getColumnIndex("uid")),
            cursor.getString(cursor.getColumnIndex("name")),
            cursor.getLong(cursor.getColumnIndex("version")),
            cursor.getInt(cursor.getColumnIndex("access")) == 1,
            cursor.getInt(cursor.getColumnIndex("foreground_access")) == 1,
            cursor.getInt(cursor.getColumnIndex("temp_access")) == 1,
            cursor.getInt(cursor.getColumnIndex("internet")) == 1,
            cursor.getInt(cursor.getColumnIndex("executable")) == 1,
            cursor.getInt(cursor.getColumnIndex("system")) == 1,
            cursor.getInt(cursor.getColumnIndex("ask")) == 1,
            TrafficType.valueOf(cursor.getString(cursor.getColumnIndex("traffic_type"))),
            cursor.getString(cursor.getColumnIndex("allow_annotations")),
            cursor.getString(cursor.getColumnIndex("blocked_annotations")),
            null
        )
    }



    /**
     * Establece en el SharedPreferences que la configuración
     * ya fué restaurada.
     * */
    fun setConfigurationRestored() {
        launch {
            preferences.edit()
                .putBoolean(DB_CONFIGURATION_RESTORED, true)
                .apply()
        }
    }

    /**
     * Indica si la configuración de la base de datos
     * ya fué restaurada.
     * */
    suspend fun isConfigurationRestored(): Boolean {
        return withContext(Dispatchers.IO) {
            preferences.getBoolean(DB_CONFIGURATION_RESTORED, false)
        }
    }

    /**
     * Establece en el dataStore la configuración del cortafuegos de la versión anterior.
     * */
    fun setFirewallLegacyConfiguration() {
        launch {
            val preferences = context.getSharedPreferences(
                "com.smartsolutions.paquetes_preferences",
                Context.MODE_PRIVATE
            )

            context.settingsDataStore.edit {
                it[PreferencesKeys.ENABLED_FIREWALL] = preferences
                    .getBoolean("firewall_running", false)
            }
        }
    }

    /**
     * Establece en el dataStore la configuración de la burbuja
     * flotante de la versión anterior.
     * */
    fun setBubbleFloatingLegacyConfiguration() {
        launch {
            val preferences = context.getSharedPreferences(
                "com.smartsolutions.paquetes_preferences",
                Context.MODE_PRIVATE
            )

            context.settingsDataStore.edit {
                it[PreferencesKeys.ENABLED_BUBBLE_FLOATING] = preferences
                    .getBoolean("widget_floating", false)
            }
        }
    }

    companion object {
        const val DB_CONFIGURATION_RESTORED = "db_configuration_restored"
    }
}