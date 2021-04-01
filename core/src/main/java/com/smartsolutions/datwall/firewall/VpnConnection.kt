package com.smartsolutions.datwall.firewall

import android.app.PendingIntent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.smartsolutions.datwall.repositories.IAppRepository
import com.smartsolutions.datwall.repositories.Observer
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.AppGroup
import com.smartsolutions.datwall.repositories.models.IApp
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Clase que estable la conexión vpn
 * */
@Singleton
class VpnConnection @Inject constructor(
    private val appRepository: IAppRepository
) {

    /**
     * Servicio vpn que se usa para establecer la conexión
     * */
    var service: VpnService? = null

    /**
     * Contiene la actividad que se lanza cuando
     * */
    var pendingIntent: PendingIntent? = null

    val connected: Boolean
        get() = connection?.fileDescriptor?.valid() == true

    private var isConnecting = false

    /**
     * Conexión vpn
     * */
    private var connection: ParcelFileDescriptor? = null

    /**
     * Lista de marcas de acceso
     * */
    private var marksOfAccess: MutableList<Int> = mutableListOf()

    private val observer = object : Observer() {

        override fun onChange(apps: List<IApp>) {
            if (hasUpdate(apps)) {

                establishConnection(apps)
            }
        }
    }

    fun start() {
        if (!connected) {
            if (service == null)
                throw IllegalStateException("VpnService not initialized")

            appRepository.registerObserver(this.observer)
        }
    }

    fun restart() {
        GlobalScope.launch(Dispatchers.IO) {
            val apps = appRepository.getAllByGroup()

            withContext(Dispatchers.Main) {
                establishConnection(apps)
            }
        }
    }

    fun stop() {
        appRepository.unregisterObserver(this.observer)

        if (connection != null) {
            try {
                connection?.close()
                connection = null
            } catch (e: Exception) {

            }
        }
    }

    private fun establishConnection(apps: List<IApp>) {
        if (!isConnecting) {

            isConnecting = true

            saveMarksOfAccess(apps)

            handshake(apps)?.let {

                if (connection != null) {
                    try {
                        connection?.close()
                    } catch (e: Exception) {

                    }
                }

                connection = it.establish()
            }

            isConnecting = false
        }
    }

    private fun handshake(apps: List<IApp>): VpnService.Builder? {
        return this.service?.let {
            synchronized(it) {
                val applicationName = it.packageManager.getApplicationLabel(it.applicationInfo).toString()

                val builder = it.Builder()
                    .addAddress("192.168.0.32", 32)
                    .addAddress("fd00:1:fd00:1:fd00:1:fd00:1", 128)
                    .addRoute("0.0.0.0", 0)
                    .addRoute("0:0:0:0:0:0:0:0", 0)
                    .setMtu(1500)
                    .setSession(applicationName)

                pendingIntent?.let { pendingIntent ->
                    builder.setConfigureIntent(pendingIntent)
                }

                configurePackages(builder, apps)

                builder
            }
        }
    }

    private fun configurePackages(builder: VpnService.Builder, apps: List<IApp>) {
        service?.let {
            builder.addDisallowedApplication(it.packageName)
        }
        apps.forEach { iapp ->
            if (iapp is App) {
                if (iapp.access || iapp.tempAccess)
                    try {
                        builder.addDisallowedApplication(iapp.packageName)
                    } catch (e: Exception) {

                    }
            } else if (iapp is AppGroup) {
                iapp.forEach { app ->
                    if (app.access || app.tempAccess)
                        try {
                            builder.addDisallowedApplication(app.packageName)
                        } catch (e: Exception) {

                        }
                }
            }
        }
    }

    private fun hasUpdate(apps: List<IApp>): Boolean {
        if (marksOfAccess.isEmpty() || marksOfAccess.size != apps.size)
            return true

        for (i in apps.indices) {
            if (marksOfAccess[i] != apps[i].accessHashCode())
                return true
        }
        return false
    }

    private fun saveMarksOfAccess(apps: List<IApp>) {
        apps.forEach {
            marksOfAccess.add(it.accessHashCode())
        }
    }
}