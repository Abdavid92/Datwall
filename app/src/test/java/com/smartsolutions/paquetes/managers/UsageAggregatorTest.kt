package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.managers.models.UsageOrder
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.TrafficType
import org.junit.Assert.assertEquals
import org.junit.Test

class UsageAggregatorTest {

    private fun app(name: String, uid: Int, type: TrafficType = TrafficType.International) =
        App().apply {
            packageName = "pkg.$name"
            this.uid = uid
            this.name = name
            trafficType = type
        }

    private fun traffic(uid: Int, bytes: Long) = Traffic(uid, bytes, 0, "2")

    @Test
    fun joinsTrafficWithAppsOfTheTypeAndSumsTheTotal() {
        val (total, apps) = UsageAggregator.aggregate(
            listOf(app("a", 1), app("b", 2), app("n", 3, TrafficType.National)),
            listOf(traffic(1, 100), traffic(2, 300), traffic(3, 1000)),
            TrafficType.International,
            UsageOrder.MAX_USAGE,
            "2"
        )

        assertEquals(400L, total)
        assertEquals(listOf("b", "a"), apps.map { it.name })
        assertEquals(300L, apps[0].traffic!!.totalBytes.bytes)
    }

    @Test
    fun appsWithoutTrafficGetAnEmptyOne() {
        val (total, apps) = UsageAggregator.aggregate(
            listOf(app("a", 1)), emptyList(), TrafficType.International, UsageOrder.MAX_USAGE, "2"
        )

        assertEquals(0L, total)
        assertEquals(0L, apps[0].traffic!!.totalBytes.bytes)
        assertEquals("2", apps[0].traffic!!.simId)
    }

    @Test
    fun ordersByNameOrUsage() {
        fun names(order: UsageOrder) = UsageAggregator.aggregate(
            listOf(app("b", 1), app("c", 2), app("a", 3)),
            listOf(traffic(1, 200), traffic(2, 100), traffic(3, 300)),
            TrafficType.International,
            order,
            "2"
        ).second.map { it.name }

        assertEquals(listOf("a", "b", "c"), names(UsageOrder.ALPHABETICAL))
        assertEquals(listOf("a", "b", "c"), names(UsageOrder.MAX_USAGE))
        assertEquals(listOf("c", "b", "a"), names(UsageOrder.MIN_USAGE))
    }

    /**
     * Posible error 2: antes, de dos apps con el mismo uid no se quitaba ninguna, y el
     * tráfico del uid se mostraba y se sumaba dos veces.
     * */
    @Test
    fun appsSharingAnUidAreShownAndCountedOnce() {
        val (total, apps) = UsageAggregator.aggregate(
            listOf(app("a", 1), app("a2", 1), app("a3", 1), app("b", 2)),
            listOf(traffic(1, 100), traffic(2, 50)),
            TrafficType.International,
            UsageOrder.MAX_USAGE,
            "2"
        )

        assertEquals(150L, total)
        assertEquals(listOf("a", "b"), apps.map { it.name })
    }

    @Test
    fun othersAreTheAppsUpToOnePercent() {
        val (main, others) = UsageAggregator.splitByShare(1000L, listOf(500L, 11L, 10L, 0L)) { it }

        assertEquals(listOf(500L, 11L), main)
        assertEquals(listOf(10L, 0L), others)
    }
}
