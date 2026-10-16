package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.AppListSection
import com.smartsolutions.paquetes.managers.models.AppListSectionType
import com.smartsolutions.paquetes.managers.models.AppsFilter
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.AppGroup
import com.smartsolutions.paquetes.repositories.models.IApp
import com.smartsolutions.paquetes.repositories.models.TrafficType

/**
 * Orden, agrupación y búsqueda de la lista de apps.
 * */
object AppListRules {

    /**
     * Secciones de [apps] según [filter]. Las secciones vacías se omiten y las apps de cada
     * sección se ordenan por nombre.
     * */
    fun sections(apps: List<IApp>, filter: AppsFilter): List<AppListSection> = when (filter) {
        AppsFilter.Alphabetic -> listOf(AppListSection(null, apps.sortedBy { it.name }))

        AppsFilter.InternetAccess -> listOfNotNull(
            section(AppListSectionType.ALLOWED, apps.filter { it.access }),
            section(AppListSectionType.BLOCKED, apps.filter { !it.access })
        )

        AppsFilter.TrafficType -> TrafficType.entries.mapNotNull { trafficType ->
            section(sectionOf(trafficType), apps.filter { trafficTypeOf(it) == trafficType })
        }
    }

    /**
     * Apps cuyo nombre contiene [query] (sin distinguir mayúsculas), buscando también dentro
     * de los grupos. Sin consulta, devuelve [apps] tal cual.
     * */
    fun search(apps: List<IApp>, query: String?): List<IApp> {
        if (query.isNullOrBlank())
            return apps

        val result = mutableListOf<IApp>()

        apps.forEach {
            if (it is AppGroup)
                result.addAll(it.filter { app -> app.name.contains(query, true) })
            else if (it.name.contains(query, true))
                result.add(it)
        }

        return result
    }

    private fun section(type: AppListSectionType, apps: List<IApp>): AppListSection? =
        if (apps.isEmpty()) null else AppListSection(type, apps.sortedBy { it.name })

    private fun trafficTypeOf(app: IApp): TrafficType? = when (app) {
        is App -> app.trafficType
        is AppGroup -> app.getMasterApp().trafficType
        else -> null
    }

    private fun sectionOf(trafficType: TrafficType) = when (trafficType) {
        TrafficType.Free -> AppListSectionType.FREE
        TrafficType.National -> AppListSectionType.NATIONAL
        TrafficType.Messaging -> AppListSectionType.MESSAGING
        TrafficType.International -> AppListSectionType.INTERNATIONAL
    }
}
