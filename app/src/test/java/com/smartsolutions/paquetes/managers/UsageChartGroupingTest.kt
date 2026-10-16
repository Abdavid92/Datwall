package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.DateCalendarUtils.MyTimeUnit
import com.smartsolutions.paquetes.managers.UsageChartGrouping.GeneralPeriod
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

class UsageChartGroupingTest {

    private fun at(day: Int, hour: Int, minute: Int) = Calendar.getInstance().apply {
        clear()
        set(2026, Calendar.OCTOBER, day, hour, minute)
    }.timeInMillis

    private fun usage(date: Long, bytes: Long) = UsageGeneral(date, DataType.International, bytes, "2")

    private val now = at(20, 0, 0)

    @Test
    fun periodsMapToTheirTimeUnit() {
        assertEquals(MyTimeUnit.MINUTE, UsageChartGrouping.generalTimeUnit(GeneralPeriod.TODAY))
        assertEquals(MyTimeUnit.MINUTE, UsageChartGrouping.generalTimeUnit(GeneralPeriod.YESTERDAY))
        assertEquals(MyTimeUnit.DAY, UsageChartGrouping.generalTimeUnit(GeneralPeriod.WEEK))
        assertEquals(MyTimeUnit.DAY, UsageChartGrouping.generalTimeUnit(GeneralPeriod.PACKAGE))

        assertEquals(MyTimeUnit.HOUR, UsageChartGrouping.appTimeUnit(DateCalendarUtils.PERIOD_TODAY))
        assertEquals(MyTimeUnit.DAY, UsageChartGrouping.appTimeUnit(DateCalendarUtils.PERIOD_MONTH))
        assertEquals(MyTimeUnit.MONTH, UsageChartGrouping.appTimeUnit(DateCalendarUtils.PERIOD_YEAR))
    }

    @Test
    fun emptyListHasNoPoints() {
        assertEquals(emptyList<UsageGeneral>(), UsageChartGrouping.compact(emptyList(), MyTimeUnit.MINUTE, now))
    }

    /**
     * Posible error 1: antes se perdía el último grupo (solo quedaba el punto vacío final).
     * */
    @Test
    fun groupsByHourKeepingEveryGroupIncludingTheLast() {
        val result = UsageChartGrouping.compact(
            listOf(
                usage(at(15, 10, 5), 10),
                usage(at(15, 10, 40), 20),
                usage(at(15, 11, 0), 5),
                usage(at(15, 12, 30), 7),
                usage(at(15, 12, 45), 3)
            ),
            MyTimeUnit.MINUTE,
            now
        )

        assertEquals(listOf(30L, 5L, 10L, 0L), result.map { it.bytes })
        assertEquals(now, result.last().date)
    }

    @Test
    fun groupsByDay() {
        val result = UsageChartGrouping.compact(
            listOf(usage(at(15, 1, 0), 10), usage(at(15, 23, 0), 20), usage(at(16, 9, 0), 5)),
            MyTimeUnit.DAY,
            now
        )

        assertEquals(listOf(30L, 5L, 0L), result.map { it.bytes })
    }

    @Test
    fun doesNotChangeTheOriginalRecords() {
        val first = usage(at(15, 10, 5), 10)

        UsageChartGrouping.compact(listOf(first, usage(at(15, 10, 6), 20)), MyTimeUnit.MINUTE, now)

        assertEquals(10L, first.bytes)
    }
}
