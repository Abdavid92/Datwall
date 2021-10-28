package com.smartsolutions.paquetes.watcher

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.await
import com.smartsolutions.paquetes.helpers.LegacyConfigurationHelper
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.workers.DbSynchronizationWorker
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
    private val context: Context,
    private val appRepository: IAppRepository,
    private val legacyConfiguration: LegacyConfigurationHelper
) {

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
            sequenceNumber = it.sequenceNumber

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
    fun forceSynchronization() {

        val request = OneTimeWorkRequestBuilder<DbSynchronizationWorker>()
            .build()

        WorkManager.getInstance(context)
            .enqueue(request)
    }

    @Suppress("DEPRECATION")
    private fun versionNotEquals(version: Long, info: PackageInfo): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            version != info.longVersionCode
        } else
            version != info.versionCode.toLong()
    }

    companion object {
        private var sequenceNumber: Int = 0

        private const val TAG = "PackageMonitor"
    }
}

enum class ChangeType {
    Created,
    Updated,
    Deleted,
    None
}