package com.smartsolutions.datwall.repositories

import com.smartsolutions.datwall.repositories.models.IApp

/**
 * Observador que está pendiente de los cambios en base de datos
 * */
interface Observer {

    /**
     * Evento que se lanza cuando hay un cambio en la base de datos
     *
     * @param apps - Lista de aplicaciones guardadas en base de datos con los últimos cambios aplicados
     * */
    fun change(apps: List<IApp>)
}