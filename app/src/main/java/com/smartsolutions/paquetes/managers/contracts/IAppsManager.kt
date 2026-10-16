package com.smartsolutions.paquetes.managers.contracts

import com.smartsolutions.paquetes.managers.models.AppListSection
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import kotlinx.coroutines.flow.Flow

/**
 * Apps instaladas para la UI: lista, orden y agrupación, búsqueda y cambios de acceso.
 * */
interface IAppsManager {

    /**
     * Apps agrupadas por uid, actualizadas con cada cambio.
     * */
    fun flowApps(): Flow<List<IApp>>

    /**
     * Secciones de [apps] según [filter].
     * */
    fun sections(apps: List<IApp>, filter: AppsFilter): List<AppListSection>

    /**
     * Apps cuyo nombre contiene [query].
     * */
    fun search(apps: List<IApp>, query: String?): List<IApp>

    /**
     * Todas las apps, sin agrupar.
     * */
    suspend fun all(): List<App>

    /**
     * Apps (sin agrupar) de los uids indicados.
     * */
    suspend fun byUids(uids: IntArray): List<App>

    /**
     * Guarda los cambios de acceso de [apps].
     * */
    suspend fun commit(apps: List<IApp>)
}
