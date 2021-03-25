package com.smartsolutions.paquetes.repositories

import androidx.lifecycle.LiveData
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp

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
     *
     * @return LiveData con la lista de aplicaciones
     * */
    fun getAll(): LiveData<List<IApp>>

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
    suspend fun update(apps: List<IApp>, task: (apps: List<App>) -> Unit)

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
}