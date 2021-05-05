package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.helpers.SimsHelper
import com.smartsolutions.paquetes.micubacel.MCubacelClient
import com.smartsolutions.paquetes.micubacel.models.ProductGroup
import com.smartsolutions.paquetes.repositories.contracts.IMiCubacelAccountRepository
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import kotlin.Exception
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.jvm.Throws

/**
 * Administrador del cliente mi.cubacel.net.
 * Los métodos de este administrador funcionan de manera
 * suspendida.
 * */
class MiCubacelClientManager @Inject constructor(
    private val miCubacelAccountRepository: IMiCubacelAccountRepository,
    private val client: MCubacelClient,
    private val simsHelper: SimsHelper,
): CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    /**
     * Carga la página principal.
     * */
    @Throws(UnprocessableRequestException::class)
    suspend fun loadHomePage(): Map<String, String> {
        setCookiesForAccount()
        val url = sendRequests(9) { client.resolveHomeUrl() }

        val page = sendRequests(9) { client.loadPage(url) }

        val result = client.readHomePage(page)

        if (result.isEmpty())
            throw UnprocessableRequestException(UnprocessableRequestException.Reason.NO_LOGIN)

        return result
    }

    /**
     * Inicia sesión.
     * */
    @Throws(UnprocessableRequestException::class)
    suspend fun signIn(phone: String, password: String) {
        val cookies = sendRequests(9) { client.signIn(phone, password) }
        miCubacelAccountRepository.createOrUpdate(MiCubacelAccount(
            simsHelper.getActiveDataSimIndex(),
            phone, password, cookies
        ))
    }

    /**
     * Inicia el proceso de creación de cuenta.
     *
     * @param firstName - Nombres.
     * @param lastName - Apellidos.
     * @param phone - Teléfono.
     * */
    suspend fun signUp(firstName : String, lastName : String, phone: String) {
        sendQueueRequests(9) { client.signUp(firstName, lastName, phone) }
    }

    /**
     * Verifica el código recibido por sms.
     *
     * @param code - Codigo recibido
     * */
    @Throws(UnprocessableRequestException::class)
    suspend fun verifyCode(code: String) {
        sendQueueRequests(9) { client.verifyCode(code) }
    }

    /**
     * Completa el proceso de creación de la cuenta con una contraseña.
     *
     * @param password - Contraseña.
     * */
    suspend fun createPassword(password: String) {
        sendQueueRequests(9) { client.createPassword(password) }
    }

    /**
     * Obtiene los datos del usuario.
     * */
    suspend fun getUserDataPackagesInfo() {
        setCookiesForAccount()
    }

    /**
     * Obtiene una lista de productos a la venta.
     * */
    suspend fun getProducts(): List<ProductGroup> {
        setCookiesForAccount()
        return sendRequests(9) { client.getProducts() }
    }

    /**
     * Compra un producto.
     *
     * @param url - Url del producto a comprar.
     * */
    suspend fun buyProduct(url: String) {
        setCookiesForAccount()
        val urlConfirmation = sendQueueRequests(9) { client.resolveUrlBuyProductConfirmation(url) }
        sendQueueRequests(9) { client.buyProduct(urlConfirmation) }
    }

    private suspend fun setCookiesForAccount() {
        val sim = simsHelper.getActiveDataSimIndex()

        miCubacelAccountRepository.get(sim).firstOrNull()?.let {
            MCubacelClient.cookies = it.cookies.toMutableMap()
        }
    }

    /**
     * Ejecuta una cantidad específica de peticiones http de manera paralela.
     * Cuando se recibe la primera respuesta, se cancelan todas las demas peticiones y
     * se lanzan los eventos correspondiente del callback.
     * En caso de no recibir respuesta a ninguna de las peticiones realizadas,
     * se lanza el evento fail del callback.
     *
     * @param attempt - Número de peticiones a ejecutar.
     * @param request - Función que contiene el código de la petición http.
     * @param T - Tipo de respuesta que retorna la petición.
     * */
    private suspend inline fun <T> sendRequests(attempt: Int, crossinline request: () -> T?): T {
        return suspendCancellableCoroutine {
            var fails = 1

            GlobalScope.launch(coroutineContext) {
                for (i in 1..attempt) {
                    if (it.isCompleted || it.isCancelled)
                        break

                    launch {
                        try {
                            if (!it.isCompleted && !it.isCancelled) {
                                it.resume(request() ?: throw NullPointerException())
                                it.cancel()
                            }

                        } catch (e: UnprocessableRequestException) {

                            if (!it.isCompleted && !it.isCancelled) {
                                it.resumeWithException(e)
                                it.cancel()
                            }

                        } catch (e: Exception) {
                            if (fails + 1 == attempt) {

                                if (!it.isCompleted && !it.isCancelled) {
                                    it.resumeWithException(e)
                                    it.cancel()
                                }
                            } else
                                fails++
                        }
                    }
                    delay(500L)
                }
            }
        }
    }

    /**
     * Ejecuta una petición http una cantidad de veces determinada mientras no tenga éxito.
     * Deja de ejecutar la peticion si se supera la cantidad de intentos o  si la peticion
     * tiene respuesta.
     *
     * @param attempt - Número de intentos.
     * @param request - Petición http.
     * */
    private suspend inline fun <T> sendQueueRequests(attempt: Int, crossinline request: () -> T?): T {
        return suspendCoroutine {
            for (i in 1..attempt) {
                try {

                    it.resume(request() ?: throw NullPointerException())

                    break
                } catch (e: UnprocessableRequestException) {
                    it.resumeWithException(e)
                    break
                } catch (e: Exception) {
                    if (i == attempt) {
                        it.resumeWithException(e)
                    }
                }
            }
        }
    }
}