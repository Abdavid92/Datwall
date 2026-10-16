package com.smartsolutions.paquetes.helpers

import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Formato común de cantidades de datos, porcentajes y fechas. Sin textos: la UI pone las
 * etiquetas (por ejemplo, "Desconocido" cuando no hay fecha).
 * */
object DataFormatter {

    /**
     * Valor y unidad más adecuada, con dos decimales.
     * */
    fun bytes(bytes: Long): DataUnitBytes.DataValue = DataUnitBytes(bytes).getValue()

    /**
     * "1.5 GB", "200.0 MB"…
     * */
    fun bytesText(bytes: Long): String = DataUnitBytes(bytes).toString()

    /**
     * Porcentaje entero que representa [part] de [total]; 0 si alguno no es positivo.
     * */
    fun percent(total: Long, part: Long): Int =
        DateCalendarUtils.calculatePercent(total.toDouble(), part.toDouble())

    /**
     * Día y mes de vencimiento ("06 noviembre"), o null si no hay fecha.
     * */
    fun expiryDate(expiredTime: Long, locale: Locale = Locale.getDefault()): String? =
        if (expiredTime == 0L)
            null
        else
            SimpleDateFormat("dd MMMM", locale).format(Date(expiredTime))

    /**
     * Tiempo que falta de [now] a [until] en la mayor unidad (días, horas o minutos);
     * (0, horas) si ya pasó.
     * */
    fun timeLeft(now: Long, until: Long): Pair<Int, TimeUnit> =
        DateCalendarUtils.calculateDiffDate(now, until)
}
