package com.smartsolutions.paquetes.firewall

import android.app.PendingIntent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import com.smartsolutions.paquetes.repositories.contracts.IAppRepository
import com.smartsolutions.paquetes.repositories.Observer
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Clase que estable la conexión vpn. Este vpn
 * actúa como un bloqueador de la conexión a internet, ya que
 * no se conecta a ningún servidor externo. Será como
 * un callejón sin salida. Todas las
 * aplicaciones que estén incluidas en el no tendrán acceso a internet.
 * */
@Singleton
class VpnConnection @Inject constructor(
    private val appRepository: IAppRepository
): CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    /**
     * Servicio vpn que se usa para establecer la conexión
     * */
    var service: VpnService? = null

    /**
     * Contiene la actividad que se lanza cuando
     * */
    var pendingIntent: PendingIntent? = null

    /**
     * Indica si el vpn está conectado o no
     * */
    val connected: Boolean
        get() = connection?.fileDescriptor?.valid() == true

    /**
     * Indica si se está estableciendo una conexión
     * */
    private var isConnecting = false

    /**
     * Conexión vpn
     * */
    private var connection: ParcelFileDescriptor? = null

    /**
     * Lista de marcas de acceso. Contienen las marcas de acceso de las
     * aplicaciones. Se usa para saber si existen diferencias de acceso
     * en los lotes de aplicaciones que provee el observador.
     * */
    private var marksOfAccess: MutableList<Long> = mutableListOf()

    /**
     * Observador que se registra en el repositorio.
     * */
    private val observer = object : Observer() {

        override fun onChange(apps: List<IApp>) {
            //Si hay diferencias en las marcas de acceso
            if (hasUpdate(apps)) {

                //Establezco la conexión
                establishConnection(apps)
            }
        }
    }

    /**
     * Inicia la conexión
     *
     * @see restart
     * @see stop
     * */
    fun start() {
        //Si no se estableció una conexión previa
        if (!connected) {
            //Verifico que el servicio no sea nulo
            if (service == null)
                throw IllegalStateException("VpnService not initialized")

            /*Registro el observador. Cuando se registra un observador
            * en el repositorio, se lanza el método onChange si la base de datos
            * ya está sembrada. Este método establece la conexión llamando al
            * método establishConnection.*/
            //appRepository.registerObserver(this.observer)
        }
    }

    /**
     * Reinicia la conexión. La conexión debe estar establecida previamente
     * para poder reiniciarla. De lo contrario no hace nada.
     *
     * @see start
     * @see stop
     * */
    fun restart() {
        if (connected) {
            launch(Dispatchers.IO) {
                val apps = appRepository.getAllByGroup()

                withContext(Dispatchers.Main) {
                    establishConnection(apps)
                }
            }
        }
    }

    /**
     * Detiene la conexión.
     *
     * @see start
     * @see restart
     * */
    fun stop() {
        //Elimino el observer del registro del repsitorio
        //appRepository.unregisterObserver(this.observer)

        /*Limpio las marcas de acceso para poder iniciar el vpn nuevamente.
        * De lo contrario el vpn no inicia porque no encuentra diferencia
        * entre las marcas de acceso.*/
        marksOfAccess.clear()

        //Si la connexión no es nula, la cierro
        if (connection != null) {
            try {
                connection?.close()
                connection = null
            } catch (e: Exception) {

            }
        }
    }

    /**
     * Establece la conexión. Cierra la conexión previamente establecida y
     * guarda las marcas de acceso.
     *
     * @param apps - Lista de aplicaciones que se configuran con el vpn
     * */
    private fun establishConnection(apps: List<IApp>) {
        //Si no se está estableciendo una conexión en este instante
        if (!isConnecting) {

            //Levanto la bandera para prevenir conexiones múltiples
            isConnecting = true

            //Guardo las marcas de acceso
            saveMarksOfAccess(apps)

            //Preparo el Builder
            handshake(apps)?.let {

                //Intento cerrar conexiones previas
                if (connection != null) {
                    try {
                        connection?.close()
                    } catch (e: Exception) {

                    }
                }

                /*
                * Si todas las aplicaciones tienen acceso a internet, no tiene sentido
                * encender el vpn. De esta manera se queda apagado mientras no haya
                * ninguna aplicación con el acceso restringido.
                * */
                if (!VpnConnectionUtils.allAccess(apps)) {
                    //Establezco la conexión
                    connection = it.establish()
                }
            }

            //Bajo la bandera
            isConnecting = false
        }
    }

    /**
     * Prepara y configura el Builder
     * */
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

    /**
     * Configura los nombres de paquetes que estarán dentro y fuera del vpn.
     *
     * @param builder - Builder en el que se configuran los nombres de paquetes.
     * @param apps - Lista de aplicaciones con los nombres de paquetes y los criterios de acceso.
     * */
    private fun configurePackages(builder: VpnService.Builder, apps: List<IApp>) {
        /*
        * De manera predeterminada todas la aplicaciones entran en el vpn
        * si se llama al método addDisallowedApplication menos la aplicación
        * que se paso como parámetro a dicho método. Para activar este modo
        * se excluye el nombre de paquete de la propia aplicación. Asi garantizamos
        * el acceso a internet de la aplicación y correcto funcionamiento del vpn.
        * */
        service?.let {
            builder.addDisallowedApplication(it.packageName)
        }
        apps.forEach { iapp ->
            //Si es una instancia de App
            if (iapp is App) {
                //Y tiene acceso permanente o temporal
                if (iapp.access || iapp.tempAccess)
                    try {
                        //La excluyo del vpn
                        builder.addDisallowedApplication(iapp.packageName)
                    } catch (e: Exception) {

                    }
            /*Si es una instancia de AppGroup itero por cada app del grupo y
            * aplico las mismas reglas.*/
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

    /**
     * Verifica que las marcas de acceso o el tamaño de la lista de aplicaciones sean diferentes.
     *
     * @return true si hay diferencias, false en caso contrario
     * */
    private fun hasUpdate(apps: List<IApp>): Boolean {
        if (marksOfAccess.isEmpty() || marksOfAccess.size != apps.size)
            return true

        for (i in apps.indices) {
            if (marksOfAccess[i] != apps[i].accessHashCode())
                return true
        }
        return false
    }

    /**
     * Guarda las marcas de acceso.
     * */
    private fun saveMarksOfAccess(apps: List<IApp>) {
        //Limpio la lista de marcas para evitar duplicados
        marksOfAccess.clear()

        apps.forEach {
            marksOfAccess.add(it.accessHashCode())
        }
    }
}