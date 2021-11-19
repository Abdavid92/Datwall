package com.smartsolutions.paquetes.workers

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.helpers.LegacyConfigurationHelper
import com.smartsolutions.paquetes.managers.contracts.IIconManager
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "DbSynchronizationWorker"

@HiltWorker
class DbSynchronizationWorker @AssistedInject constructor(
    @Assisted
    context: Context,
    @Assisted
    params: WorkerParameters,
    private val appRepository: IAppRepository,
    private val legacyConfiguration: LegacyConfigurationHelper,
    private val iconManager: IIconManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        forceSynchronization()

        return Result.success()
    }

    /**
     * Fuerza la sincronización de la base de datos revisando todas las aplicaciones
     * instaladas y creando, actualizando o eliminando según corresponda.
     *
     * */
    private suspend fun forceSynchronization() {

        val packageManager = applicationContext.packageManager

        Log.i(TAG, "forceSynchronization: obtain installed apps")
        //Obtengo las aplicaciones instaladas
        val installedPackages = packageManager.getInstalledPackages(0)

        //Obtengo las aplicaciones guardadas en base de datos
        val apps = appRepository.all()

        val appsToCreateOrReplace = mutableListOf<App>()
        val appsToDelete = mutableListOf<App>()

        Log.i(TAG, "forceSynchronization: synchronize apps")

        installedPackages.forEach { info ->
            //Por cada aplicación instalada, busco en la aplicaciones guardadas.
            var app = apps.firstOrNull { it.packageName == info.packageName }

            if (app == null) {
                /*Si no la encuentro la instancio, la lleno
                * y la guardo en la base de datos.*/
                app = App()

                appRepository.fillNewApp(app, info)
                appsToCreateOrReplace.add(app)
            } else if (versionNotEquals(app.version, info)) {
                /*Pero si la encuentro reviso que la version sea diferente.
                * Si lo es, la actualizo.*/
                appRepository.fillApp(app, info)
                appsToCreateOrReplace.add(app)
            }
        }

        Log.i(TAG, "forceSynchronization: find uninstalled apps")

        //Esta iteración es para buscar las aplicaciones que han sido desinstaladas.
        apps.forEach { app ->
            //Si no esta dentro de las aplicaciones instaladas, la elimino de la base de datos.
            if (installedPackages.firstOrNull { app.packageName == it.packageName } == null) {
                appsToDelete.add(app)
            }
        }

        Log.i(TAG, "forceSynchronization: finish the finding of uninstalled apps")

        Log.i(TAG, "forceSynchronization: creating or replacing apps")
        appRepository.createOrReplace(appsToCreateOrReplace)

        Log.i(TAG, "forceSynchronization: deleting uninstall apps")
        appRepository.delete(appsToDelete)

        Log.i(TAG, "forceSynchronization: update access of groups")
        //Actualizo los accesos a los grupos
        appRepository.getAllByGroup()
            .filterIsInstance<AppGroup>()
            .forEach { group ->

                group.access = group.getMasterApp().access

                appRepository.update(group)
            }

        Log.i(TAG, "forceSynchronization: restoring old configuration")
        restoreOldConfiguration()

        Log.i(TAG, "forceSynchronization: synchronizing icons")
        iconManager.synchronizeIcons(installedPackages)

        Log.i(TAG, "forceSynchronization: finish of synchronization")
    }

    /**
     * Restaura las configuraciones de la versión anterior de la aplicación.
     * */
    private suspend fun restoreOldConfiguration() {
        if (!legacyConfiguration.isConfigurationRestored()) {

            val apps = appRepository.all()
                .filter { !it.access }

            val updateApps = mutableListOf<App>()

            legacyConfiguration.getLegacyRules().forEach { packageName ->
                apps.firstOrNull { it.packageName == packageName }?.let {
                    updateApps.add(it.apply {
                        access = true
                    })
                }
            }

            if (updateApps.isNotEmpty())
                appRepository.update(updateApps)

            legacyConfiguration.setConfigurationRestored()
        }
    }

    @Suppress("DEPRECATION")
    private fun versionNotEquals(version: Long, info: PackageInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            version != info.longVersionCode
        } else
            version != info.versionCode.toLong()
    }
}