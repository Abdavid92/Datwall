package com.smartsolutions.paquetes.managers

/**
 * Qué hacer cuando el usuario pulsa "Conceder" sobre un permiso.
 * */
enum class PermissionAction {
    Nothing,
    Request,
    OpenAppSettings
}

object PermissionRequestPolicy {

    /**
     * @param granted el permiso ya está concedido.
     * @param requestedBefore la app lo pidió alguna vez.
     * @param canAskAgain el sistema deja volver a preguntar (`shouldShowRequestPermissionRationale`).
     * Antes de la primera petición también es `false`, por eso hace falta [requestedBefore].
     * */
    fun decide(granted: Boolean, requestedBefore: Boolean, canAskAgain: Boolean): PermissionAction =
        when {
            granted -> PermissionAction.Nothing
            !requestedBefore || canAskAgain -> PermissionAction.Request
            else -> PermissionAction.OpenAppSettings
        }
}
