package com.smartsolutions.paquetes.watcher

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.repositories.IAppRepository
import com.smartsolutions.paquetes.repositories.models.App
import javax.inject.Inject

@RequiresApi(Build.VERSION_CODES.O)
class PackageMonitor @Inject constructor(
    private val packageManager: PackageManager,
    private val appRepository: IAppRepository
) {

    private var sequenceNumber: Int = 0

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
                    if (app != null && app.version != info.versionName) {
                        //Lleno la aplicación existente
                        appRepository.fillApp(app, info)
                        //La actualizo en base de datos
                        appRepository.update(app)
                    } else {
                        //Instancio una nueva App
                        app = App()
                        //
                        appRepository.fillNewApp(app, info)
                        appRepository.create(app)
                        //TODO: Aquí debo lanza un evento para actualizar el vpn
                    }
                } catch (e: PackageManager.NameNotFoundException) {
                    //Si no encontré el packageInfo es porque fué desinstalada
                    if (app != null)
                        appRepository.delete(app)
                    //TODO: Aquí debo lanza un evento para actualizar el vpn
                }
            }
        }
    }
}