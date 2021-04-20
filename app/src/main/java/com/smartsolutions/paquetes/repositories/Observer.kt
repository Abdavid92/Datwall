package com.smartsolutions.paquetes.repositories

import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp

/**
 * Observador que está pendiente de los cambios en base de datos
 * */
abstract class Observer {

    /**
     * Evento que se lanza cuando hay un cambio en la base de datos
     *
     * @param apps - Lista de aplicaciones guardadas en base de datos con los últimos cambios aplicados
     * */
    open fun onChange(apps: List<IApp>) {

    }

    /**
     * Evento que se lanza cuando se crea una o varias aplicaciones
     *
     * @param apps - Aplicaciones que se crearon
     * */
    open fun onCreate(apps: List<App>) {

    }

    /**
     * Evento que se lanza cuando se actualiza una o varias aplicaciones
     *
     * @param apps - Aplicaciones que se actualizaron
     * */
    open fun onUpdate(apps: List<App>) {

    }

    /**
     * Evento que se lanza cuando se elimina una o varias aplicaciones
     *
     * @param apps - Aplicaciones que se eliminaron
     * */
    open fun onDelete(apps: List<App>) {

    }
}