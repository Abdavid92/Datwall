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
    fun get(packageName: String, size: Int = DEFAULT_IMAGE_SIZE): Bitmap?

    /**
     * Obtiene el ícono de la aplicación actualizado a la versión actual de esta
     * de forma asíncrona.
     *
     * @param packageName - Nombre de paquete de la aplicación
     * @param callback - Callback con el resultado. Si no se encuentra nada, no se ejecuta
     * este callback.
     *
     * */
    fun getAsync(packageName: String, size: Int = DEFAULT_IMAGE_SIZE, callback: (img: Bitmap) -> Unit)

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
    fun get(packageName: String, versionCode: Long, size: Int = DEFAULT_IMAGE_SIZE): Bitmap?

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
    fun getAsync(
        packageName: String,
        versionCode: Long,
        size: Int = DEFAULT_IMAGE_SIZE,
        callback: (img: Bitmap) -> Unit)

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
    fun getImageFile(packageName: String, versionCode: Long, size: Int = DEFAULT_IMAGE_SIZE): File?

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
    fun getImageFileAsync(
        packageName: String,
        versionCode: Long,
        size: Int = DEFAULT_IMAGE_SIZE,
        callback: (img: File) -> Unit)

    /**
     * Elimina todas las versiones del ícono que coincida con el
     * nombre de paquete y el versionCode.
     *
     * @param packageName - Nombre de paquete.
     * @param versionCode - VersionCode de la aplicacion a la que pertenece el ícono.
     * */
    fun delete(packageName: String, versionCode: Long)

    /**
     * Elimina la cache de íconos completa.
     * */
    fun deleteAll()

    companion object {
        const val DEFAULT_IMAGE_SIZE = 50
    }
}