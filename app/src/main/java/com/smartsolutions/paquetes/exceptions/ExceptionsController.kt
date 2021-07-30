package com.smartsolutions.paquetes.exceptions

import javax.inject.Inject

class ExceptionsController @Inject constructor(

) : Thread.UncaughtExceptionHandler {

    var isRegistered = false
        private set

    override fun uncaughtException(t: Thread, e: Throwable) {
        if (e is MissingPermissionException) {
            throw e
        } else
            throw e
    }

    fun register() {
        if (!isRegistered) {
            Thread.setDefaultUncaughtExceptionHandler(this)
            isRegistered = true
        }
    }

    fun unregister() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        isRegistered = false
    }
}