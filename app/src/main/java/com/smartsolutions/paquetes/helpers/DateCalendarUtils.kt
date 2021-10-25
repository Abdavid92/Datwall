package com.smartsolutions.paquetes.helpers

import androidx.annotation.IntDef
import com.smartsolutions.paquetes.managers.contracts.IPurchasedPackagesManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang.time.DateUtils
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.IllegalArgumentException

/**
 * Métodos de utilidad para NetworkUsageManager.
 * */
class DateCalendarUtils @Inject constructor(
   private val purchasedPackagesManager: IPurchasedPackagesManager,
    private val simManager: ISimManager
) {




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
    fun getTimePeriod(@Period trafficPeriod: Int): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val currentTime = System.currentTimeMillis()
        return when (trafficPeriod) {
            PERIOD_TODAY -> {
                Pair(
                    getZeroHour(Date()).time,
                   currentTime
                )
            }
            PERIOD_YESTERDAY -> {
                var yesterday = DateUtils.addDays(Date(), -1)
                yesterday = DateUtils.setHours(yesterday, 23)
                yesterday = DateUtils.setMinutes(yesterday, 59)
                yesterday = DateUtils.setSeconds(yesterday, 59)

                Pair(
                    getZeroHour(yesterday).time,
                    yesterday.time
                )
            }
            PERIOD_WEEK -> {
                calendar.firstDayOfWeek = Calendar.MONDAY
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)

                Pair(
                    getZeroHour(calendar.time).time,
                    currentTime
                )
            }
            PERIOD_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)

                Pair(
                    getZeroHour(calendar.time).time,
                    currentTime
                )
            }
            PERIOD_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)

                Pair(
                    getZeroHour(calendar.time).time,
                    currentTime
                )
            }
            PERIOD_PACKAGE -> {
                runBlocking(Dispatchers.Default) {
                    purchasedPackagesManager.getHistory().firstOrNull()?.filter {
                        it.simId == simManager.getDefaultSim(SimDelegate.SimType.DATA).id
                    }?.maxByOrNull { it.date }?.let {
                        return@runBlocking it.date to System.currentTimeMillis()
                    }
                    return@runBlocking Pair(0L, 0L)
                }
            }
            else -> {
                throw IllegalArgumentException("Unknown period")
            }
        }
    }


    companion object {

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

        /**
         * Este Año
         */
        const val PERIOD_YEAR = 4

        /**
         * Desde el ultimo paquete comprado
         */
        const val PERIOD_PACKAGE = 5

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
            date = DateUtils.setMilliseconds(date, 0)

            return date
        }

        fun getLastHour (day : Date) : Date {
            var date = day
            date = DateUtils.setHours(date, 23)
            date = DateUtils.setMinutes(date, 59)
            date = DateUtils.setSeconds(date, 59)
            date = DateUtils.setMilliseconds(date, 999)

            return date
        }

        fun isSameMinute(date: Long, minuteLong: Long): Boolean {
            var minute = DateUtils.setSeconds(Date(minuteLong), 0)
            minute = DateUtils.setMilliseconds(minute, 0)

            val start = minute.time

            minute = DateUtils.setSeconds(minute, 59)
            minute = DateUtils.setMilliseconds(minute, 999)

            val finish = minute.time

            return date in start..finish
        }

        fun isSameHour(date: Long, hourLong: Long): Boolean {
            var hour = DateUtils.setMinutes(Date(hourLong), 0)
            hour = DateUtils.setSeconds(hour, 0)
            hour = DateUtils.setMilliseconds(hour, 0)

            val start = hour.time

            hour = DateUtils.setMinutes(hour, 59)
            hour = DateUtils.setSeconds(hour, 59)
            hour = DateUtils.setMilliseconds(hour, 999)

            val finish = hour.time

            return date in start..finish
        }

        fun isSameDay(date: Long, dayLong: Long): Boolean {
            val day = Date(dayLong)
            val start = getZeroHour(day).time
            val finish = getLastHour(day).time

            return date in start..finish
        }

        fun isSameMonth(date: Long, month: Long): Boolean {
            val start = getZeroHour(DateUtils.setDays(Date(month), 1)).time
            val calendar = Calendar.getInstance()
            calendar.time = Date(month)
            val finish = getLastHour(
                DateUtils.setDays(
                    calendar.time,
                    calendar.getActualMaximum(Calendar.DATE)
                )
            ).time
            calendar.clear()
            return date in start..finish
        }

        /**
         * Calcula el porcentaje.
         * @param total - Valor base.
         * @param part - Valor relativo.
         * */
        fun calculatePercent(total : Double, part : Double): Int {
            return (100 * part / total).toInt()
        }

        fun isInDiscountHour (start: Long, finish: Long) : Boolean{
            var startTime = DateUtils.setHours(Date(start), 1)
            startTime = DateUtils.setMinutes(startTime, 0)
            startTime = DateUtils.setSeconds(startTime, 1)

            var finishTime = DateUtils.setHours(Date(start), 6)
            finishTime = DateUtils.setMinutes(finishTime, 0)
            finishTime = DateUtils.setSeconds(finishTime, 1)

            return Date(start).after(startTime) && Date(finish).before(finishTime)
        }

        fun calculateDiffDate(first: Long, second: Long): Pair<Int, TimeUnit> {
            val rest = second - first
            if (rest > 0) {
               for (i in 0..2){
                   val unit = when(i) {
                       0 -> DateUtils.MILLIS_PER_DAY
                       1 -> DateUtils.MILLIS_PER_HOUR
                       2 -> DateUtils.MILLIS_PER_MINUTE
                       else -> 1
                   }
                   val cuantity = rest / unit

                   if (cuantity > 0){
                       return Pair(cuantity.toInt(), when(i) {
                           0 -> TimeUnit.DAYS
                           1 -> TimeUnit.HOURS
                           else -> TimeUnit.MINUTES
                       })
                   }
               }
            }

            return Pair(0, TimeUnit.HOURS)
        }

        fun TimeUnit.nameLegible(): String {
           return when(this) {
                TimeUnit.DAYS -> "Días"
                TimeUnit.HOURS -> "Horas"
                TimeUnit.MINUTES -> "Minutos"
                else -> ""
            }
        }

    }

    enum class MyTimeUnit {
        MONTH,
        DAY,
        HOUR,
        MINUTE
    }

}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
@IntDef(
    DateCalendarUtils.PERIOD_TODAY,
    DateCalendarUtils.PERIOD_YESTERDAY,
    DateCalendarUtils.PERIOD_WEEK,
    DateCalendarUtils.PERIOD_MONTH,
    DateCalendarUtils.PERIOD_YEAR,
    DateCalendarUtils.PERIOD_PACKAGE
)
annotation class Period