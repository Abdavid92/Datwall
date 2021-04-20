package com.smartsolutions.paquetes.managers

import androidx.annotation.IntDef
import org.apache.commons.lang.time.DateUtils
import java.util.*
import kotlin.IllegalArgumentException

/**
 * Métodos de utilidad para NetworkUsageManager.
 * */
object NetworkUtils {

    /**
     * Calcula el porcentaje.
     * @param total - Valor base.
     * @param part - Valor relativo.
     * */
    fun calculatePercent(total : Double, part : Double) : Int{
        return (100 * part / total).toInt()
    }

    /**
     * Obtiene la hora cero de un día.
     *
     * @param day - Día que se le va a obtener la hora
     * */
    fun getZeroHour (day : Date) : Date {
        var date = day
        date = try {
            DateUtils.setHours(date, 0)
        }catch (e : IllegalArgumentException){
            DateUtils.setHours(date, 1)
        }
        date = DateUtils.setMinutes(date, 0)
        date = DateUtils.setSeconds(date, 1)

        return date
    }

    /**
     * Obtiene diferentes periodos de tiempo para sacarles las estadísticas de tráfico.
     *
     * @param trafficPeriod - Constante que determina el periodo a obtener.
     *
     * @return Un arreglo de dos elemento con el tiempo de inicio y el tiempo de
     * finalización.
     *
     * @see PERIOD_TODAY
     * @see PERIOD_YESTERDAY
     * @see PERIOD_WEEK
     * @see PERIOD_MONTH
     * */
    fun getTimePeriod(@Period trafficPeriod: Int): Array<Long> {
        return when (trafficPeriod) {
            0 -> {
                arrayOf(
                        getZeroHour(Date()).time,
                        System.currentTimeMillis()
                )
            }
            1 -> {
                var yesterday = DateUtils.addDays(Date(), -1)
                yesterday = DateUtils.setHours(yesterday, 23)
                yesterday = DateUtils.setMinutes(yesterday, 59)
                yesterday = DateUtils.setSeconds(yesterday, 59)

                arrayOf(
                        getZeroHour(yesterday).time,
                        yesterday.time
                )
            }
            2 -> {
                val calendar = Calendar.getInstance()
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

                arrayOf(
                        getZeroHour(calendar.time).time,
                        System.currentTimeMillis()
                )
            }
            3 -> {
                val  calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, 1)

                arrayOf(
                        getZeroHour(calendar.time).time,
                        System.currentTimeMillis()
                )
            }
            else -> {
                throw IllegalArgumentException("Unknown period")
            }
        }
    }

    /**
     * Día de hoy
     * */
    const val PERIOD_TODAY = 0

    /**
     * Día de ayer
     * */
    const val PERIOD_YESTERDAY = 1

    /**
     * Esta semana
     * */
    const val PERIOD_WEEK = 2

    /**
     * Este mes
     * */
    const val PERIOD_MONTH = 3

}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
@IntDef(
    NetworkUtils.PERIOD_TODAY,
    NetworkUtils.PERIOD_YESTERDAY,
    NetworkUtils.PERIOD_WEEK,
    NetworkUtils.PERIOD_MONTH
)
annotation class Period