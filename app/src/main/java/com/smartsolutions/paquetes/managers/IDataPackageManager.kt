package com.smartsolutions.paquetes.managers

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataPackage

/**
 * Administrador de paquetes de datos.
 * Se encarga de comprar, guardar la compra, el historial
 * y otras funciones más.
 * */
interface IDataPackageManager {

    /**
     * Obtiene todos los paquetes disponibles para
     * la compra.
     *
     * @return LiveData con los paquetes disponibles para la compra.
     * */
    fun getPackages(): LiveData<DataPackage>

    /**
     * Obtiene el paquete que está en uso actualmente.
     *
     * @return `UserDataPackage` o null si no hay ningún
     * paquete activo.
     * */
    fun getUserDataPackage(): UserDataPackage?

    /**
     * Compra un paquete de datos.
     *
     * @return `true` si la compra tuvo éxito
     * */
    fun buyDataPackage(id: Int): Boolean

    /**
     * Establece el modo de compra.
     *
     * @param mode - Modo de compra.
     * */
    fun setBuyMode(mode: BuyMode)

    /**
     * Obtiene el historial de todos los paquetes comprados.
     *
     * @return List con todos los paquetes comprados.
     * */
    fun getHistory(): List<UserDataPackage>

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