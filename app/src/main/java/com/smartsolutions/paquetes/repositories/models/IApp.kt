package com.smartsolutions.paquetes.repositories.models

import android.os.Parcelable

interface IApp: Parcelable {
    /**
     * Nombre de paquete
     * */
    var packageName: String
    /**
     * Identificador único (uid)
     * */
    var uid: Int
    /**
     * Nombre de la aplicación
     * */
    var name: String
    /**
     * Acceso permanente
     * */
    var access: Boolean
    /**
     * Indica si este IApp pertenece al sistema.
     * */
    var system: Boolean
    /**
     * Anotación de advertencia que se muestra cuando se intenta conceder
     * el acceso permanente.
     * */
    var allowAnnotations: String?
    /**
     * Anotación de advertencia que se muestra cuando se intenta bloquear
     * el acceso permanente.
     * */
    var blockedAnnotations: String?

    /**
     * @return Un número construido basandose en el
     * acceso permanente (access) y el acceso temporal (tempAccess)
     * en el caso de la implementación [App]
     * */
    fun accessHashCode(): Long
}