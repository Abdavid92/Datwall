package com.smartsolutions.paquetes.watcher

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.helpers.LegacyConfigurationHelper
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Se encarga de actualizar el repositorio con
 * todos los cambios que ocurran en las aplicaciones del sistema.
 * */
@Singleton
class PackageMonitor @Inject constructor(
    @ApplicationContext
    context: Context,
    private val appRepository: IAppRepository,
    private val legacyConfiguration: LegacyConfigurationHelper
) {

    private var sequenceNumber: Int = 0

    private val packageManager = context.packageManager

    /**
     * Obtiene las aplicaciones cambiadas del PackageManager.
     * Si encuentra alguna averigua cuales fueron los cambios que tuvieron y
     * actualiza el repositorio.
     * */
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun synchronizeDatabase() {

        //Reviso que existan aplicaciones con cambios
        packageManager.getChangedPackages(sequenceNumber)?.let {
            //Si existen aplicaciones con cambios

            //Guardo la última secuencia
            this.sequenceNumber = it.sequenceNumber

            //Por cada aplicación
            it.packageNames.forEach { packageName ->
                //La obtengo del repositorio
                var app = appRepository.get(packageName)

                try {
                    //Obtengo el packageInfo
                    val info = packageManager.getPackageInfo(packageName, 0)

                    //Si la versión de la aplicacion guardada en base de datos es diferente del packageInfo
                    if (app != null && versionNotEquals(app.version, info)) {
                        //Lleno la aplicación existente
                        appRepository.fillApp(app, info)
                        //La actualizo en base de datos
                        appRepository.update(app)
                    } else if (app == null) {
                        //Instancio una nueva App
                        app = App()
                        //La lleno
                        appRepository.fillNewApp(app, info)
                        //Y la creo
                        appRepository.create(app)
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    //Si no encontré el packageInfo es porque fué desinstalada
                    if (app != null) {
                        appRepository.delete(app)
                    }
                }
            }
        }
    }

    /**
     * Inserta, actualiza o elimina una aplicación dada por el nombre de paquete.
     * La acción aplicada a la aplicación se determina por el changeType parámetro dado.
     *
     * @param packageName - Nombre de paquete de la aplicación a tratar
     * @param changeType - Tipo de cambio que se va a aplicar
     * */
    suspend fun synchronizeDatabase(packageName: String, changeType: ChangeType) {
        when (changeType) {
            ChangeType.Created -> {
                val app = App()
                val info = packageManager.getPackageInfo(packageName, 0)

                appRepository.fillNewApp(app, info)
                appRepository.create(app)
            }
            ChangeType.Updated -> {
                var app = appRepository.get(packageName)
                val info = packageManager.getPackageInfo(packageName, 0)

                if (app != null) {
                    appRepository.fillApp(app, info)
                    appRepository.update(app)
                } else {
                    app = App()

                    appRepository.fillNewApp(app, info)
                    appRepository.create(app)
                }
            }
            ChangeType.Deleted -> {
                appRepository.get(packageName)?.let {
                    appRepository.delete(it)
                }
            }
            else -> {
                //None
            }
        }
    }

    /**
     * Fuerza la sincronización de la base de datos revisando todas las aplicaciones
     * instaladas y creando, actualizando o eliminando según corresponda.
     *
     * */
    suspend fun forceSynchronization() {
        //Obtengo las aplicaciones instaladas
        val installedPackages = packageManager.getInstalledPackages(0)

        //Obtengo las aplicaciones guardadas en base de datos
        val apps = appRepository.all()

        //val appsToAdd = mutableListOf<App>()
        //val appsToUpdate = mutableListOf<App>()
        val appsToCreateOrReplace = mutableListOf<App>()
        val appsToDelete = mutableListOf<App>()

        installedPackages.forEach { info ->
            //Por cada aplicación instalada, busco en la aplicaciones guardadas.
            var app = apps.firstOrNull { it.packageName == info.packageName }

            if (app == null) {
                /*Si no la encuentro la instancio, la lleno
                * y la guardo en la base de datos.*/
                app = App()

                appRepository.fillNewApp(app, info)
                //appsToAdd.add(app)
                appsToCreateOrReplace.add(app)
            } else if (versionNotEquals(app.version, info)) {
                /*Pero si la encuentro reviso que la version sea diferente.
                * Si lo es, la actualizo.*/
                appRepository.fillApp(app, info)
                //appsToUpdate.add(app)
                appsToCreateOrReplace.add(app)
            }
        }

        //Esta iteración es para buscar las aplicaciones que han sido desinstaladas.
        apps.forEach { app ->
            //Si no esta dentro de las aplicaciones instaladas, la elimino de la base de datos.
            if (installedPackages.firstOrNull { app.packageName == it.packageName } == null) {
                appsToDelete.add(app)
            }
        }

        //appRepository.create(appsToAdd)
        //appRepository.update(appsToUpdate)
        appRepository.createOrReplace(appsToCreateOrReplace)
        appRepository.delete(appsToDelete)

        //Actualizo los accesos a los grupos
        appRepository.getAllByGroup()
            .filterIsInstance<AppGroup>()
            .forEach { group ->

            group.access = group.getMasterApp().access

            appRepository.update(group)
        }

        restoreOldConfiguration()
    }

    /**
     * Restaura las configuraciones de la versión anterior de la aplicación.
     * */
    @Deprecated("En la próxima versión se eliminara la retro-compatibilidad")
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

    private fun versionNotEquals(version: Long, info: PackageInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            version != info.longVersionCode
        } else
            version != info.versionCode.toLong()
    }
}

enum class ChangeType {
    Created,
    Updated,
    Deleted,
    None
}