package com.smartsolutions.paquetes.data

import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.Sim

/**
 * Qué planes se ofrecen en una línea.
 * */
object PackagesRules {

    enum class Availability {
        /**
         * Hay planes para comprar.
         * */
        AVAILABLE,

        /**
         * La línea está configurada pero no tiene planes: es una línea empresarial.
         * */
        NO_PLANS,

        /**
         * La línea no se ha configurado.
         * */
        NOT_CONFIGURED
    }

    /**
     * Planes que se muestran: ninguno si la línea no tiene red o no tiene planes.
     * */
    fun visiblePlans(sim: Sim): List<DataPackage> =
        if (sim.network == Networks.NETWORK_NONE) emptyList() else sim.packages

    fun availability(sim: Sim): Availability = when {
        visiblePlans(sim).isNotEmpty() -> Availability.AVAILABLE
        sim.setupDate > 0L -> Availability.NO_PLANS
        else -> Availability.NOT_CONFIGURED
    }
}
