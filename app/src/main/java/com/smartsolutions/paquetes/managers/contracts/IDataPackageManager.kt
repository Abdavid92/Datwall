package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim
import kotlin.jvm.Throws

/**
 * Administrador de paquetes de datos.
 * Se encarga de comprar, guardar la compra, el historial
 * y otras funciones más.
 * */
interface IDataPackageManager {

    /**
     * Modo de compra que se va a usar en el
     * momento de intentar comprar un paquete de datos.
     * Puede se por ussd o por mi.cubacel.net.
     * */
    var buyMode: ConnectionMode

    /**
     * Configura los paquetes de datos en dependencia
     * de los que tenga disponible la tarjeta sim. Este método
     * elige la linea predeterminada para llamadas para realizar la configuración.
     *
     * @throws USSDRequestException en caso de que no tenga permiso de llamada,
     * el servicio de accesibilidad no esté encendido o la respuesta agote el
     * tiempo de espera.
     * */
    @Throws(USSDRequestException::class)
    suspend fun configureDataPackages()

    /**
     * Compra un paquete de datos.
     *
     * @param dataPackage - Paquete que se va a intentar comprar.
     * @param sim - Linea por donde se va a comprar el paquete.
     *
     * @throws MissingPermissionException si no tiene los permisos necesarios.
     *
     * @throws IllegalStateException si el paquete no se puede comprar
     * porque no está configurado o no está activado para la linea actual.
     * 
     * @throws UnprocessableRequestException si la compra
     * no tuvo éxito por algún problema de red.
     *
     * @throws NoSuchElementException si se esta comprando por mi.cubacel.net y no existe una cuenta asociada
     * a la linea.
     * */
    @Throws(
        IllegalStateException::class,
        MissingPermissionException::class,
        UnprocessableRequestException::class,
        NoSuchElementException::class)
    suspend fun buyDataPackage(dataPackage: DataPackage, sim: Sim)

    /**
     * Registra un paquete comprado y actualiza los corresppondientes
     * repositorios.
     *
     * @param smsBody - Cuerpo del mensage recibido como confirmación
     * del paquete comprado.
     * @param simIndex - Index o slot de la linea por donde entró el mensaje.
     * */
    suspend fun registerDataPackage(smsBody: String, simIndex: Int)

    /**
     * Modo de compra y sincronización de los paquetes.
     * */
    enum class ConnectionMode {
        /**
         * Por código ussd
         * */
        USSD,
        /**
         * Por mi.cubacel.net
         * */
        MiCubacel,
        /**
         * Origen desconocido
         * */
        Unknown
    }
}