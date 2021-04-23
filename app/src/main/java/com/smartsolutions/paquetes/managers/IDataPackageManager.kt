package com.smartsolutions.paquetes.managers

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.repositories.models.DataPackage

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
    fun getPackages(): LiveData<List<DataPackage>>

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