package com.smartsolutions.datwall

/**
 * Callback que se usa para retornar de manera asíncrona el resultado de
 * una petición http.
 * */
interface ResponseCallback<T> {

    fun onSuccess(response: T)

    fun onFail(th: Throwable? = null)
}