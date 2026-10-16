package com.smartsolutions.paquetes.managers.models

import com.smartsolutions.paquetes.repositories.models.IApp

/**
 * Cabecera de una sección de la lista de apps. La UI pone el texto.
 * */
enum class AppListSectionType {
    ALLOWED,
    BLOCKED,
    FREE,
    NATIONAL,
    MESSAGING,
    INTERNATIONAL
}

/**
 * Una sección de la lista de apps; sin cabecera si [type] es null.
 * */
data class AppListSection(val type: AppListSectionType?, val apps: List<IApp>)
