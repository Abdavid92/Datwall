package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.micubacel.MCubacelClient
import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import com.smartsolutions.paquetes.repositories.contracts.IMiCubacelAccountRepository
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import com.smartsolutions.paquetes.serverApis.models.Result
import javax.inject.Inject

/**
 * Administrador del cliente mi.cubacel.net.
 * Los métodos de este administrador funcionan de manera
 * suspendida.
 * */
class MiCubacelManager @Inject constructor(
    private val miCubacelAccountRepository: IMiCubacelAccountRepository,
    private val client: MCubacelClient,
    private val simManager: SimManager,
    private val userDataBytesManager: IUserDataBytesManager
): AbstractMiCubacelManager() {

    private val attempts = 9

    /**
     * Inicia sesión.
     *
     * */
    override suspend fun signIn(account: MiCubacelAccount): Result<Unit> {
        return try {
            account.cookies = sendRequests(attempts) {
                client.signIn(account.phone, account.password)
            }

            miCubacelAccountRepository.createOrUpdate(account)

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    /**
     * Inicia el proceso de creación de cuenta.
     *
     * @param firstName - Nombres.
     * @param lastName - Apellidos.
     * @param phone - Teléfono.
     * */
    override suspend fun signUp(firstName: String, lastName: String, account: MiCubacelAccount): Result<MiCubacelAccount> {
        return try {
            account.cookies = sendQueueRequests(attempts) { client.signUp(firstName, lastName, account.phone) }

            Result.Success(account)
        }catch (e: Exception){
            Result.Failure(e)
        }
    }

    /**
     * Verifica el código recibido por sms.
     *
     * @param code - Codigo recibido
     * */
    override suspend fun verifyCode(code: String, account: MiCubacelAccount): Result<MiCubacelAccount> {
        return try {
            account.cookies = sendQueueRequests(attempts) { client.verifyCode(code) }
            Result.Success(account)
        }catch (e: Exception){
            Result.Failure(e)
        }
    }

    /**
     * Completa el proceso de creación de la cuenta con una contraseña.
     *
     * @param password - Contraseña.
     * */
    override suspend fun createPassword(account: MiCubacelAccount): Result<MiCubacelAccount> {
        return try {
            account.cookies = sendQueueRequests(attempts) {client.createPassword(account.password)}
            miCubacelAccountRepository.createOrUpdate(account)
            Result.Success(account)
        }catch (e: Exception){
            Result.Failure(e)
        }
    }

    override suspend fun synchronizeUserDataBytes(account: MiCubacelAccount?): Result<Unit> {
        val account1 = account ?: miCubacelAccountRepository.get(simManager.getDefaultDataSim().id)

        account1?.let {
            return try {
                getUserDataBytes(account1)
                Result.Success(Unit)

            } catch (e: Exception) {
                loginAndRetry(e, account1) { getUserDataBytes(account1) }
            }
        }

        return Result.Failure(NoSuchElementException())
    }

    /**
     * Obtiene una lista de productos a la venta.
     * */
    override suspend fun getProducts(account: MiCubacelAccount): Result<List<ProductGroup>> {
        client.COOKIES = account.cookies.toMutableMap()

        return try {
            Result.Success(
                sendRequests(attempts) { client.getProducts() }
            )
        } catch (e: Exception) {
            return loginAndRetry(e, account) { client.getProducts() }
        }
    }

    /**
     * Compra un producto.
     *
     * @param url - Url del producto a comprar.
     * */
    override suspend fun buyProduct(url: String, account: MiCubacelAccount): Result<Unit> {
        client.COOKIES = account.cookies.toMutableMap()

        return try {
            val urlConfirmation = sendQueueRequests(attempts) {
                client.resolveUrlBuyProductConfirmation(url)
            }

            sendQueueRequests(attempts) {
                client.buyProduct(urlConfirmation)
            }

            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Failure(e)
        }
    }

    private suspend fun getUserDataBytes(account: MiCubacelAccount) {
        client.COOKIES = account.cookies.toMutableMap()
        val data = sendRequests(attempts) { client.obtainPackagesInfo() }
        userDataBytesManager.synchronizeUserDataBytes(data, account.simId)
    }
}