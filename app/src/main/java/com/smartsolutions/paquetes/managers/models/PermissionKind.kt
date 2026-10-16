package com.smartsolutions.paquetes.managers.models

/**
 * Cómo se concede un permiso. La UI decide qué pantalla del sistema abre en cada caso.
 * */
enum class PermissionKind {
    /**
     * Permiso del sistema con su diálogo.
     * */
    RUNTIME,

    /**
     * Acceso de uso: se concede en los ajustes del sistema.
     * */
    USAGE_ACCESS,

    /**
     * Mostrar sobre otras apps: se concede en los ajustes del sistema.
     * */
    OVERLAY,

    /**
     * VPN: se concede en el diálogo de VPN del sistema.
     * */
    VPN
}
