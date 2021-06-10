package com.smartsolutions.paquetes.managers.contracts

import android.graphics.Bitmap

interface IIconManager {

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión pasada como argumento
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     *
     * @return [Bitmap] - Ícono de la aplicación
     * */
    fun get(packageName: String, versionCode: Long): Bitmap

    /**
     * Elimina un ícono.
     * */
    fun delete(packageName: String, versionCode: Long)

    /**
     * Elimina la cache de íconos completa.
     * */
    fun deleteAll()
}