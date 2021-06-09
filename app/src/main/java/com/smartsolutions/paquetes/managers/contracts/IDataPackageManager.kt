package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.exceptions.NotFoundException
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
    var buyMode: BuyMode

    /**
     * Configura los paquetes de datos en dependencia
     * de los que tenga disponible la tarjeta sim.
     * */
    suspend fun configureDataPackages()

    /**
     * Compra un paquete de datos.
     *
     * @throws MissingPermissionException si no tiene los permisos necesarios.
     *
     * @throws IllegalStateException si el paquete no se puede comprar
     * porque no está configurado o no está activado para la linea actual.
     * 
     * @throws UnprocessableRequestException si la compra
     * no tuvo éxito por alguna razón.
     *
     * @throws NotFoundException si se esta comprando por mi.cubacel.net y no existe una cuenta asociada
     * a la linea.
     * */
    @Throws(
        IllegalStateException::class,
        MissingPermissionException::class,
        UnprocessableRequestException::class,
        NotFoundException::class)
    suspend fun buyDataPackage(dataPackage: DataPackage, sim: Sim)

    /**
     * Registra un paquete comprado y actualiza los corresppondientes
     * repositorios.
     *
     * @param smsBody - Cuerpo del mensage rrecibido como confirmación
     * del paquete comprado.
     * */
    suspend fun registerDataPackage(smsBody: String, simIndex: Int)

    /**
     * Modo de compra de paquetes.
     * */
    enum class BuyMode {
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