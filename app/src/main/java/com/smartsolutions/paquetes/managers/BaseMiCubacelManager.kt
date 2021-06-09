package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.exceptions.UnprocessableRequestException
import com.smartsolutions.paquetes.managers.contracts.IMiCubacelManager
import com.smartsolutions.paquetes.repositories.models.MiCubacelAccount
import com.smartsolutions.paquetes.serverApis.models.Result
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

abstract class BaseMiCubacelManager : IMiCubacelManager {

    protected suspend fun <T> loginAndRetry(e: Exception, account: MiCubacelAccount, action: suspend () -> T): Result<T> {
        if (e is UnprocessableRequestException && e.reason == UnprocessableRequestException.Reason.NO_LOGIN) {
            val signResult = signIn(account)

            if (signResult.isSuccess) {
                return try {
                    return Result.Success(action())
                } catch (e: Exception) {
                    Result.Failure(e)
                }
            }
            return Result.Failure((signResult as Result.Failure).throwable)
        }
        return Result.Failure(e)
    }

    /**
     * Ejecuta una cantidad específica de peticiones http de manera paralela.
     * Cuando se recibe la primera respuesta, se cancelan todas las demas peticiones y
     * se lanzan los eventos correspondiente del callback.
     * En caso de no recibir respuesta a ninguna de las peticiones realizadas,
     * se lanza el evento fail del callback.
     *
     * @param attempts - Número de peticiones a ejecutar.
     * @param request - Función que contiene el código de la petición http.
     * @param T - Tipo de respuesta que retorna la petición.
     * */
    protected suspend inline fun <T> sendRequests(attempts: Int, crossinline request: () -> T?): T {
        return suspendCancellableCoroutine {
            var fails = 1

            GlobalScope.launch(Dispatchers.IO) {
                for (i in 1..attempts) {
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
                            if (fails + 1 == attempts) {

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
     * @param attempts - Número de intentos.
     * @param request - Petición http.
     * */
    protected suspend inline fun <T> sendQueueRequests(attempts: Int, crossinline request: () -> T?): T {
        return suspendCoroutine {
            for (i in 1..attempts) {
                try {

                    it.resume(request() ?: throw NullPointerException())

                    break
                } catch (e: UnprocessableRequestException) {
                    it.resumeWithException(e)
                    break
                } catch (e: Exception) {
                    if (i == attempts) {
                        it.resumeWithException(e)
                    }
                }
            }
        }
    }
}