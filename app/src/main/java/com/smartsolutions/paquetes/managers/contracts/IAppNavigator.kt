package com.smartsolutions.paquetes.managers.contracts

import android.content.Context
import android.content.Intent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Único punto por el que servicios, notificaciones y el arranque abren la app. Lo implementa
 * la UI, que decide qué pantalla corresponde a cada destino. Devuelve el intent base: quien lo
 * usa añade sus flags.
 * */
interface IAppNavigator {

    /**
     * Entrada normal de la app.
     * */
    fun splash(): Intent

    /**
     * Configuración de las líneas.
     * */
    fun simsConfiguration(): Intent

    /**
     * Aviso de actualización disponible.
     * */
    fun updates(): Intent

    /**
     * Informe de un error de la app.
     * */
    fun exceptions(): Intent
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppNavigatorEntryPoint {
    fun appNavigator(): IAppNavigator
}

/**
 * Navegador para clases que no se pueden inyectar (métodos estáticos, objetos creados por
 * reflexión).
 * */
fun Context.appNavigator(): IAppNavigator =
    EntryPointAccessors.fromApplication(applicationContext, AppNavigatorEntryPoint::class.java)
        .appNavigator()
