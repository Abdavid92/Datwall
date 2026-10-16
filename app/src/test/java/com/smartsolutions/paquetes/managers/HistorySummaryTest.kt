package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.PurchasedPackage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class HistorySummaryTest {

    private fun at(month: Int, day: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, month, day, 12, 0)
    }.timeInMillis

    private fun purchase(id: Long, date: Long, price: Float, simId: String = "2") =
        PurchasedPackage(
            id, date, IDataPackageManager.ConnectionMode.USSD, simId, false,
            DataPackages.PackageId.DailyBag
        ).apply {
            dataPackage = mockk<DataPackage> { every { this@mockk.price } returns price }
        }

    @Test
    fun groupsTheLineHistoryByMonthNewestFirst() {
        val months = HistorySummary.months(
            listOf(
                purchase(1, at(Calendar.SEPTEMBER, 3), 25f),
                purchase(2, at(Calendar.OCTOBER, 1), 240f),
                purchase(3, at(Calendar.OCTOBER, 9), 120f),
                purchase(4, at(Calendar.OCTOBER, 5), 360f, simId = "1"),
                purchase(5, at(Calendar.SEPTEMBER, 20), 25f)
            ),
            "2"
        )

        assertEquals(2, months.size)

        assertEquals(listOf(3L, 2L), months[0].purchases.map { it.id })
        assertEquals(360, months[0].totalPrice)
        assertEquals(at(Calendar.OCTOBER, 9), months[0].date)

        assertEquals(listOf(5L, 1L), months[1].purchases.map { it.id })
        assertEquals(50, months[1].totalPrice)
    }

    @Test
    fun emptyHistoryHasNoMonths() {
        assertEquals(emptyList<HistorySummary.Month>(), HistorySummary.months(emptyList(), "2"))
    }
}
