package com.smartsolutions.datwall.managers

import androidx.annotation.IntDef
import org.apache.commons.lang.time.DateUtils
import java.lang.IllegalArgumentException
import java.util.*

object NetworkUtils {

    fun calculatePercent(total : Double, part : Double) : Int{
        return (100 * part / total).toInt()
    }

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

    fun getTimestamp(@Period trafficPeriod: Int): Array<Long> {
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
                arrayOf(
                        0L, 0L
                )
            }
        }
    }

    const val PERIOD_TODAY = 0
    const val PERIOD_YESTERDAY = 1
    const val PERIOD_WEEK = 2
    const val PERIOD_MONTH = 3

}

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
@IntDef(NetworkUtils.PERIOD_TODAY, NetworkUtils.PERIOD_YESTERDAY, NetworkUtils.PERIOD_WEEK, NetworkUtils.PERIOD_MONTH)
annotation class Period