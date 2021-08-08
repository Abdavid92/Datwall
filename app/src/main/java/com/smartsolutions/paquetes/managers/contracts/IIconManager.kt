package com.smartsolutions.paquetes.managers.contracts

import android.graphics.Bitmap
import java.io.File

interface IIconManager {

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión actual de esta.
     * Este método es más lento que el otro así que se recomienda usar la sobrecarga de este.
     *
     * @param packageName - Nombre de paquete de la aplicación.
     *
     * @return [Bitmap] - Ícono de la aplicación o null si no se encontró el ícono
     * en la cache o la aplicación a la que pertenece.
     * */
    fun get(packageName: String): Bitmap?

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión actual de esta
     * de forma asíncrona.
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     * @param callback - Callback con el resultado. Si no se encuentra nada, no se ejecuta
     * este callback.
     *
     * */
    fun getAsync(packageName: String, callback: (img: Bitmap) -> Unit)

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión pasada como argumento.
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     *
     * @return [Bitmap] - Ícono de la aplicación o null si no se encontró el ícono
     * en la cache o la aplicación a la que pertenece.
     * */
    fun get(packageName: String, versionCode: Long): Bitmap?

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión pasada como argumento
     * de forma asíncrona.
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     * @param callback - Callback con el resultado. Si no se encuentra nada, no se ejecuta
     * este callback.
     *
     * */
    fun getAsync(packageName: String, versionCode: Long, callback: (img: Bitmap) -> Unit)

    /**
     * Obtiene la imagen de la aplicación actualizado a la versión pasada como argumento.
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     *
     * @return [File] - Archivo con el ícono de la aplicación o null si no se encontró
     * la aplicación.
     * */
    fun getImageFile(packageName: String, versionCode: Long): File?

    /**
     * Obtiene la imagen de la aplicación actualizado a la versión pasada como argumento
     * de manera asíncrona.
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param versionCode - Versión de la aplicación que se usará para determinar si
     * se debe actualizar el ícono.
     * @param callback - Callback con el resultado. Si no se encuentra nada, no se ejecuta
     * este callback.
     * */
    fun getImageFileAsync(packageName: String, versionCode: Long, callback: (img: File) -> Unit)

    /**
     * Elimina un ícono.
     * */
    fun delete(packageName: String, versionCode: Long)

    /**
     * Elimina la cache de íconos completa.
     * */
    fun deleteAll()
}