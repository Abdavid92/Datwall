package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.DateCalendarUtils.Companion.isSameMonth
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage

/**
 * Historial de compras de una línea agrupado por mes.
 * */
object HistorySummary {

    data class Month(
        /**
         * Fecha de la compra más reciente del mes.
         * */
        val date: Long,
        val totalPrice: Int,
        val purchases: List<PurchasedPackage>
    )

    /**
     * Compras de [simId] de la más reciente a la más antigua, por meses, con la suma de precios.
     * */
    fun months(history: List<PurchasedPackage>, simId: String): List<Month> {
        val months = mutableListOf<MutableList<PurchasedPackage>>()

        history.filter { it.simId == simId }
            .sortedByDescending { it.date }
            .forEach { purchase ->
                val current = months.lastOrNull()
                if (current != null && isSameMonth(purchase.date, current[0].date))
                    current.add(purchase)
                else
                    months.add(mutableListOf(purchase))
            }

        return months.map { purchases ->
            Month(
                date = purchases[0].date,
                totalPrice = purchases.sumOf { it.dataPackage.price.toInt() },
                purchases = purchases
            )
        }
    }
}
