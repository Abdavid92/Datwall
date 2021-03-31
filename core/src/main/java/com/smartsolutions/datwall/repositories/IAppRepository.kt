package com.smartsolutions.datwall.repositories

import android.content.pm.PackageInfo
import androidx.lifecycle.LiveData
import com.smartsolutions.datwall.repositories.models.App
import com.smartsolutions.datwall.repositories.models.IApp

/**
 * Repositorio de aplicaciones
 * */
interface IAppRepository {

    /**
     * Cantidad total de aplicaciones
     * */
    val appsCount: Int

    /**
     * Cantidad de aplicaciones permitidas
     * */
    val appsAllowedCount: Int

    /**
     * Cantidad de aplicaciones bloqueadas
     * */
    val appsBlockedCount: Int

    /**
     * Obtiene todas las aplicaciones
     * */
    val all: List<App>

    /**
     * Registra un observador que estará a la espera de cambios en base de datos
     *
     * @param observer - Observador a registrar
     * */
    fun registerObserver(observer: Observer)

    /**
     * Elimina un observador del registro
     *
     * @param observer - Observador a eliminar
     * */
    fun unregisterObserver(observer: Observer)

    /**
     * Obtiene una aplicación por el nombre de paquete
     *
     * @param packageName - Nombre de paquete de la aplicación a buscar
     *
     * @return App o null si no la encontró
     * */
    suspend fun get(packageName: String): App?

    /**
     * Obtiene una aplicación o un grupo de ellas por el uid
     * */
    suspend fun get(uid: Int): IApp

    /**
     * Obtiene todas la aplicaciones organizadas por grupo
     * */
    suspend fun getAllByGroup(): List<IApp>

    /**
     * Inserta una aplicación en base de datos
     *
     * @param app - Aplicación a crear
     * */
    suspend fun create(app: App)

    /**
     * Inserta una aplicación y ejecuta una función
     *
     * @param app - Aplicación a insertar
     * @param task - Función que se ejecuta después de insertar la aplicación.
     * Como argumento se le pasa la aplicación que se insertó
     * */
    suspend fun create(app: App, task: (app: App) -> Unit)

    /**
     * Actualiza una aplicación
     *
     * @param app - Aplicación a actualizar
     * */
    suspend fun update(app: App)

    /**
     * Actualiza una aplicación y ejecuta una función
     *
     * @param app - Aplicación a actualizar
     * @param task - Función que se ejecuta después de actualizar la aplicación.
     * Como argumento se le pasa la aplicación que se actualizó
     * */
    suspend fun update(app: App, task: (app: App) -> Unit)

    /**
     * Actualiza una lista de aplicaciones
     *
     * @param apps - Lista de aplicaciones
     * */
    suspend fun update(apps: List<IApp>)

    /**
     * Actualiza una lista de aplicaciones y ejecuta una función
     *
     * @param apps - Lista de aplicaciones
     * @param task - Función que se ejecuta después de actualizar la lista de aplicaciones.
     * Recibe esta lista como argumento
     * */
    suspend fun update(apps: List<IApp>, task: (apps: List<IApp>) -> Unit)

    /**
     * Elimina una aplicación
     *
     * @param app - Aplicación a eliminar
     * */
    suspend fun delete(app: App)

    /**
     * Elimina una aplicación y ejecuta una función
     *
     * @param app - Aplicación a eliminar
     * @param task - Función que se ejecuta después de eliminar la aplicación
     * */
    suspend fun delete(app: App, task: (app: App) -> Unit)

    /**
     * Llena una nueva aplicación con los datos del PackageInfo.
     * Ademas resuelve otros datos necesarios. Este método no se debe usar
     * en una aplicación existente porque restablece todos los valores de esta.
     * En su lugar use el método fillApp
     *
     * @param app - Aplicación a llenar
     * @param info - PackageInfo que se usará para llenar la aplicación
     *
     * @see fillApp
     * */
    fun fillNewApp(app: App, info: PackageInfo)

    /**
     * LLena una aplicación existente con los datos del PackageInfo.
     * Este no debe usarse en una nueva aplicación porque no llenará todos los datos
     * para no restablecer los valores mutables. Mas bien debe usarse para actualizar
     * los datos de una aplicación que ha sido actualizada en el sistema.
     *
     * @param app - Aplicación a llenar
     * @param info - PackageInfo que se usará para llenar la aplicación
     *
     * @see fillNewApp
     * */
    fun fillApp(app: App, info: PackageInfo)
}