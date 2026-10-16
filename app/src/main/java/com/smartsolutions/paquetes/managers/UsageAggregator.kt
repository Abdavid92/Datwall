package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.managers.models.UsageOrder
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType

/**
 * Une el tráfico de datos con las apps para la pantalla de consumo.
 * */
object UsageAggregator {

    /**
     * Consumo de las apps de [type]: total en bytes y apps en el orden pedido, cada una con su
     * tráfico (vacío en [simId] si no tiene). Las apps que comparten uid comparten el tráfico,
     * así que solo se muestra la primera de cada uid.
     * */
    fun aggregate(
        apps: List<App>,
        traffics: List<Traffic>,
        type: TrafficType,
        order: UsageOrder,
        simId: String
    ): Pair<Long, List<App>> {
        val ofType = apps.filter { it.trafficType == type }.distinctBy { it.uid }

        var total = 0L

        ofType.forEach { app ->
            app.traffic = traffics.firstOrNull { it.uid == app.uid }
            total += app.traffic?.totalBytes?.bytes ?: 0

            if (app.traffic == null) {
                app.traffic = Traffic(app.uid, 0, 0, simId)
            }
        }

        val sorted = when (order) {
            UsageOrder.ALPHABETICAL -> ofType.sortedBy { it.name }
            UsageOrder.MAX_USAGE -> ofType.sortedByDescending { it.traffic!!.totalBytes.bytes }
            UsageOrder.MIN_USAGE -> ofType.sortedBy { it.traffic!!.totalBytes.bytes }
        }

        return total to sorted
    }

    /**
     * Separa [items] entre los que pasan del 1 % de [total] y el resto ("otras").
     * */
    fun <T> splitByShare(total: Long, items: List<T>, bytes: (T) -> Long): Pair<List<T>, List<T>> {
        val onePercent = total / 100
        return items.partition { bytes(it) > onePercent }
    }
}
