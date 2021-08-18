package com.smartsolutions.paquetes.repositories.contracts

import android.content.pm.PackageInfo
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de aplicaciones
 * */
interface IAppRepository {

    /**
     * Cantidad total de aplicaciones
     * */
    suspend fun appsCount(): Int

    /**
     * Cantidad de aplicaciones permitidas
     * */
    suspend fun appsAllowedCount(): Int

    /**
     * Cantidad de aplicaciones bloqueadas
     * */
    suspend fun appsBlockedCount(): Int

    /**
     * Obtiene todas las aplicaciones
     * */
    suspend fun all(): List<App>

    /**
     * Retorna un Flow con la lista de aplicaciones.
     *
     * @return Flow con la lista de aplicaciones.
     * */
    fun flow(): Flow<List<App>>

    fun flowByGroup(): Flow<List<IApp>>

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
    suspend fun get(uid: Int): IApp?

    suspend fun get(uid: IntArray): List<App>

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
     * Inserta una lista de aplicaciones
     *
     * @param apps - Lista de aplicaciones a insertar
     * */
    suspend fun create(apps: List<IApp>)

    /**
     * Actualiza una aplicación
     *
     * @param app - Aplicación a actualizar
     * */
    suspend fun update(app: App): Int

    /**
     * Actualiza una lista de aplicaciones
     *
     * @param apps - Lista de aplicaciones
     * */
    suspend fun update(apps: List<IApp>): Int

    /**
     * Elimina una aplicación
     *
     * @param app - Aplicación a eliminar
     * */
    suspend fun delete(app: App): Int

    /**
     * Elimina una lista de aplicaciones
     *
     * @param apps - Lista de aplicaciones a eliminar
     * */
    suspend fun delete(apps: List<IApp>): Int

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