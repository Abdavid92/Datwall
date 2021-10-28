package com.smartsolutions.paquetes.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.smartsolutions.paquetes.watcher.ChangeType
import com.smartsolutions.paquetes.watcher.PackageMonitor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Receiver que se encarga de monitorear los cambios en las
 * aplicaciones instaladas. Este receiver funciona de api 24 para abajo.
 * En las demas apis se usa un bucle infinito.
 * */
@AndroidEntryPoint
class PackageMonitorReceiver : BroadcastReceiver(), CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    /**
     * Monitor de paquetes. Este se encarga de sincronizar la base de datos con los
     * nuevos cambios.
     * */
    @Inject
    lateinit var packageMonitor: PackageMonitor

    override fun onReceive(context: Context, intent: Intent) {

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
            val packageName = intent.data?.encodedSchemeSpecificPart

            packageName?.let {

                //Resuelvo el tipo de cambio
                val changeType: ChangeType = when (intent.action) {
                    Intent.ACTION_PACKAGE_ADDED -> ChangeType.Created
                    Intent.ACTION_PACKAGE_REPLACED -> ChangeType.Updated
                    Intent.ACTION_PACKAGE_FULLY_REMOVED -> ChangeType.Deleted
                    else -> ChangeType.None
                }

                launch {
                    /*Sincronizo la base de datos con el nombre de paquete
                     * de la aplicación afectada y el tipo de cambio que se produjo.
                     * */
                    packageMonitor.synchronizeDatabase(it, changeType)
                }
            }
        }
    }
}