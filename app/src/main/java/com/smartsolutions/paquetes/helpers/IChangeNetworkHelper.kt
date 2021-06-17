package com.smartsolutions.paquetes.helpers

import android.content.Context

/**
 * Interfaz de ayuda para cambiar el estado de la aplicación en dependencia
 * del cambio de red.
 * */
interface IChangeNetworkHelper {

    /**
     * Se llama cuando los datos móbiles se encendieron.
     * Cambia el estado de la aplicación en relación con los datos móbiles encendidos.
     *
     * Guarda un valor en el DatwallApplication que dice
     * que los datos móbiles están encendidos.
     *
     * */
    fun setDataMobileStateOn()

    /**
     * Se llama cuando los datos móbiles se apagaron.
     * Cambia el estado de la aplicación en relación con los datos móbiles apagados.
     *
     * Guarda un valor en el DatwallApplication que dice
     * que los datos móbiles están apagados.
     *
     * */
    fun setDataMobileStateOff()
}