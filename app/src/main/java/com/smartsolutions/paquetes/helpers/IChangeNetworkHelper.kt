package com.smartsolutions.paquetes.helpers

import android.content.Context
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.*

/**
 * Interfaz de ayuda para cambiar el estado de la aplicación en dependencia
 * del cambio de red.
 * */
interface IChangeNetworkHelper {

    /**
     * Se llama cuando los datos móbiles se encendieron.
     * Cambia el estado de la aplicación en relación con los datos móbiles encendidos.
     *
     * Guarda un valor en el Context.dataStore con la clave DATA_MOBILE_ON que dice
     * que los datos móbiles están encendidos.
     *
     * @see Context.dataStore
     * @see PreferencesKeys.DATA_MOBILE_ON
     * */
    fun setDataMobileStateOn()

    /**
     * Se llama cuando los datos móbiles se apagaron.
     * Cambia el estado de la aplicación en relación con los datos móbiles apagados.
     *
     * Guarda un valor en el Context.dataStore con la clave DATA_MOBILE_ON que dice
     * que los datos móbiles están apagados.
     *
     * @see Context.dataStore
     * @see PreferencesKeys.DATA_MOBILE_ON
     * */
    fun setDataMobileStateOff()
}