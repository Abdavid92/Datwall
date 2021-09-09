package com.smartsolutions.paquetes.helpers

import androidx.annotation.IntDef
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import kotlinx.coroutines.runBlocking
import org.apache.commons.lang.time.DateUtils
import java.util.*
import javax.inject.Inject
import kotlin.IllegalArgumentException

/**
 * Métodos de utilidad para NetworkUsageManager.
 * */
class NetworkUsageUtils @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
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
                runBlocking {
                    userDataBytesRepository.bySimId(simManager.getDefaultSim(SimDelegate.SimType.DATA).id).firstOrNull { it.exists() && !it.isExpired()}?.let {
                        return@let Pair(it.startTime, currentTime)
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

            return date
        }

        /**
         * Calcula el porcentaje.
         * @param total - Valor base.
         * @param part - Valor relativo.
         * */
        fun calculatePercent(total : Double, part : Double) : Int{
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

    }

    enum class TimeUnit {
        MONTH,
        DAY,
        HOUR
    }

}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
@IntDef(
    NetworkUsageUtils.PERIOD_TODAY,
    NetworkUsageUtils.PERIOD_YESTERDAY,
    NetworkUsageUtils.PERIOD_WEEK,
    NetworkUsageUtils.PERIOD_MONTH,
    NetworkUsageUtils.PERIOD_YEAR,
    NetworkUsageUtils.PERIOD_PACKAGE
)
annotation class Period