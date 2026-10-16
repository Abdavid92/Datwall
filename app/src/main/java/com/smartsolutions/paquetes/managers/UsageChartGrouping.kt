package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.DateCalendarUtils.MyTimeUnit
import com.smartsolutions.paquetes.repositories.models.UsageGeneral

/**
 * Agrupación de las gráficas de consumo.
 * */
object UsageChartGrouping {

    /**
     * Periodos de la gráfica de consumo general, en el orden de su selector.
     * */
    enum class GeneralPeriod {
        TODAY,
        YESTERDAY,
        WEEK,
        MONTH,
        PACKAGE
    }

    /**
     * Unidad de la gráfica general: por horas hoy y ayer, por días en el resto.
     * */
    fun generalTimeUnit(period: GeneralPeriod): MyTimeUnit = when (period) {
        GeneralPeriod.TODAY, GeneralPeriod.YESTERDAY -> MyTimeUnit.MINUTE
        else -> MyTimeUnit.DAY
    }

    /**
     * Unidad de la gráfica de una app según el periodo elegido en Consumo.
     * */
    fun appTimeUnit(period: Int): MyTimeUnit = when (period) {
        DateCalendarUtils.PERIOD_TODAY,
        DateCalendarUtils.PERIOD_YESTERDAY -> MyTimeUnit.HOUR
        DateCalendarUtils.PERIOD_WEEK,
        DateCalendarUtils.PERIOD_MONTH,
        DateCalendarUtils.PERIOD_PACKAGE -> MyTimeUnit.DAY
        DateCalendarUtils.PERIOD_YEAR -> MyTimeUnit.MONTH
        else -> MyTimeUnit.HOUR
    }

    /**
     * Une el consumo consecutivo de la misma hora ([MyTimeUnit.MINUTE]) o del mismo día, y
     * cierra la serie con un punto vacío en [now].
     * */
    fun compact(list: List<UsageGeneral>, timeUnit: MyTimeUnit, now: Long = System.currentTimeMillis()): List<UsageGeneral> {
        val compacted = mutableListOf<UsageGeneral>()

        var group: UsageGeneral? = null

        list.forEach { usage ->
            val current = group
            if (current == null) {
                group = usage.copy()
            } else {
                val isSame = if (timeUnit == MyTimeUnit.MINUTE) {
                    DateCalendarUtils.isSameHour(usage.date, current.date)
                } else {
                    DateCalendarUtils.isSameDay(usage.date, current.date)
                }

                if (isSame) {
                    current += usage
                } else {
                    compacted.add(current)
                    group = usage.copy()
                }
            }
        }

        group?.let { compacted.add(it) }

        if (list.isNotEmpty()) {
            compacted.add(UsageGeneral(now, list[0].type, 0L, list[0].simId))
        }

        return compacted
    }
}
