package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import com.smartsolutions.paquetes.serverApis.models.Result

/**
 * Administrador del cliente mi.cubacel.net.
 * Los métodos de este administrador funcionan de manera
 * suspendida.
 * */
interface IMiCubacelManager {

    /**
     * Inicia sesión.
     *
     * @param account - Cuenta de mi.cubacel.net
     *
     * @return [Result]
     * */
    suspend fun signIn(account: MiCubacelAccount): Result<Unit>

    /**
     * Inicia el proceso de creación de cuenta.
     *
     * @param firstName - Nombres.
     * @param lastName - Apellidos.
     * @param phone - Teléfono.
     *
     * @return [Result]
     * */
    suspend fun signUp(firstName : String, lastName : String, account: MiCubacelAccount): Result<MiCubacelAccount>

    /**
     * Verifica el código recibido por sms.
     *
     * @param code - Codigo recibido.
     *
     * @return [Result]
     * */
    suspend fun verifyCode(code: String, account: MiCubacelAccount): Result<MiCubacelAccount>

    /**
     * Completa el proceso de creación de la cuenta con una contraseña.
     *
     * @param password - Contraseña.
     *
     * @return [Result] - Si el resultado es exitoso, se retorna la cuenta creada.
     * */
    suspend fun createPassword(account: MiCubacelAccount): Result<MiCubacelAccount>

    /**
     * Sincroniza los UserDataBytes de la cuenta.
     *
     * @param account - Cuenta a sincronizar.
     * Si es null se sincriniza la cuenta predeterminada.
     * */
    suspend fun synchronizeUserDataBytes(account: MiCubacelAccount? = null): Result<Unit>

    /**
     * Obtiene una lista de productos a la venta.
     *
     * @param account - Cuenta a la que se le van a obtener los productos.
     *
     * @return [Result]
     * */
    suspend fun getProducts(account: MiCubacelAccount): Result<List<ProductGroup>>

    /**
     * Compra un producto.
     *
     * @param url - Url del producto a comprar.
     * */
    suspend fun buyProduct(url: String, account: MiCubacelAccount): Result<Unit>
}