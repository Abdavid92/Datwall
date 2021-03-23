package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.core.content.ContextCompat

abstract class Manager(protected val context: Context) {

    protected fun <T> getSystemService(clazz: Class<T>): T {
        return ContextCompat.getSystemService(context, clazz) ?: throw IllegalArgumentException("Invalid service name for ${clazz.name}")
    }
}