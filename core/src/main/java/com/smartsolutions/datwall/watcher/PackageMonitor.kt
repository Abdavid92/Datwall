package com.smartsolutions.datwall.watcher

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.smartsolutions.datwall.repositories.IAppRepository
import com.smartsolutions.datwall.repositories.models.App
import javax.inject.Inject

/**
 * Se encarga de mantener el repositorio actualizado con
 * todos los cambios que ocurran en las aplicaciones del sistema.
 * */
class PackageMonitor @Inject constructor(
    private val packageManager: PackageManager,
    private val appRepository: IAppRepository
) {

    private var sequenceNumber: Int = 0

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
                        //LA lleno
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
     * Ejecuta una función al final del proceso.
     *
     * @param task - Función a ejecutar cuando termina el proceso de sincronización
     * */
    suspend fun forceSynchronization(task: (() -> Unit)? = null) {
        val installedPackages = packageManager.getInstalledPackages(0)

        val apps = appRepository.all

        installedPackages.forEach { info ->
            var app = apps.firstOrNull { it.packageName == info.packageName }

            if (app == null) {
                app = App()

                appRepository.fillNewApp(app, info)
                appRepository.create(app)
            } else if (versionNotEquals(app.version, info)) {
                appRepository.fillApp(app, info)
                appRepository.update(app)
            }
        }

        apps.forEach { app ->
            if (installedPackages.firstOrNull { app.packageName == it.packageName } == null) {
                appRepository.delete(app)
            }
        }
        task?.invoke()
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