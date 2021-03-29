package com.smartsolutions.datwall.managers

import android.content.Context
import androidx.core.content.ContextCompat

/**
 * Clase base para las clases administradoras
 * */
abstract class Manager(protected val context: Context) {

    /**
     * Obtiene un servicio de android o lanza una excepción si no pudo resolverlo
     *
     * @param clazz - Tipo de servicio a resolver
     * */
    protected fun <T> getSystemService(clazz: Class<T>): T {
        return ContextCompat.getSystemService(context, clazz) ?: throw IllegalArgumentException("Invalid service name for ${clazz.name}")
    }
}