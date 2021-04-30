package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import kotlinx.coroutines.flow.Flow
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
     * Obtiene todos los paquetes disponibles para
     * la compra.
     *
     * @return Flow con los paquetes disponibles para la compra.
     * */
    fun getPackages(): Flow<List<DataPackage>>

    /**
     * Compra un paquete de datos.
     *
     * @throws IllegalStateException Lanza una excepción si el paquete no se puede comprar
     * porque no está configurado o no está activado para la linea actual.
     * 
     * @throws UnprocessableRequestException Lanza una excepción si la compra
     * no tuvo éxito por alguna razón.
     * */
    @Throws(IllegalStateException::class, UnprocessableRequestException::class)
    suspend fun buyDataPackage(dataPackage: DataPackage)

    /**
     * Registra un paquete comprado y actualiza los corresppondientes
     * repositorios.
     *
     * @param smsBody - Cuerpo del mensage rrecibido como confirmación
     * del paquete comprado.
     * */
    fun registerDataPackage(smsBody: String)

    /**
     * Obtiene el historial de compras de paquetes.
     * */
    fun getHistory(): Flow<List<PurchasedPackage>>

    /**
     * Limpia el historial de compras.
     * */
    fun clearHistory()

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
        MiCubacel
    }
}