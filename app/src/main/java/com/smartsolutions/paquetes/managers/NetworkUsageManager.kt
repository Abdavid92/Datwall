package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import org.apache.commons.lang.time.DateUtils
import java.util.*

/**
 * Administrador de estadísticas de tráfico de datos de la aplicaciones.
 * Se encarga de recopilar y obtener el uso de datos de las aplicaciones.
 * */
abstract class NetworkUsageManager(private val simManager: ISimManager) {

    protected lateinit var simId: String

    /**
     * Obtiene el tráfico de un tiempo dado de una o varias aplicaciones por el uid.
     *
     * @param uid - Uid de la aplicación o  las aplicaciones.
     * @param start - Tiempo de inicio del tráfico de datos que se quiere obtener.
     * @param finish - Tiempo de finalización del tráfico de datos.
     *
     *
     * @return Una instancia de Traffic con los datos recopilados.
     * */
    abstract suspend fun getAppUsage(
        uid: Int,
        start: Long,
        finish: Long,
        updateSim: Boolean = true
    ): Traffic

    /**
     * Obtiene el tráfico completo de un tiempo dado organizado por aplicaciones.
     *
     * @param start - Tiempo de inicio del tráfico a obtener.
     * @param finish - Tiempo de finalización de tráfico a obtener.
     *
     * @return Una lista con instancias de Traffic organizados por uid.
     * */
    abstract suspend fun getAppsUsage(start: Long, finish: Long): List<Traffic>

    /**
     * Obtiene el tráfico de una lista de aplicaciones y se lo asigna a estas.
     *
     * @param apps - Lista de aplicaciones de las que se va a obtener el tráfico y
     * posteriormente asignar.
     * @param start - Tiempo de inicio del trafico a obtener.
     * @param finish - Tiempo de finalización.
     * */
    abstract suspend fun fillAppsUsage(apps: List<IApp>, start: Long, finish: Long)

    /**
     * Obtiene el tráfico completo de un tiempo dado.
     *
     * @param start - Tiempo de inicio del tráfico a obtener.
     * @param finish - Tiempo de finalización de tráfico a obtener.
     *
     * @return Una instancia de Traffic con los datos recopilados. Esta instancia no tendrá
     * asignado ningún uid porque se trata del trafico completo en el tiempo dado.
     * */
    abstract suspend fun getUsageTotal(start: Long, finish: Long): Traffic

    /**
     * Método de utilidad para obtener la aplicacion de la lista que más o menos consumió en
     * un tiempo dado.
     *
     * @param apps - Aplicaciones a analizar.
     * @param start - Tiempo de inicio.
     * @param finish - Tiempo de finalización.
     * @param moreConsumed - `true` si se quiere obtener la que más consumió, `false` si se quiere obtener
     * la que menos consumió.
     *
     * @return La aplicación resultante de la búsqueda.
     * */
    suspend fun getAppPerConsumed(
        apps: List<App>,
        start: Long,
        finish: Long,
        moreConsumed: Boolean
    ): App? {
        if (apps.isEmpty()) {
            return null
        }
        fillAppsUsage(apps, start, finish)
        var app = apps[0]
        for (i in 1..apps.size) {
            if (moreConsumed) {
                if (apps[i].traffic!! > app.traffic!!) {
                    app = apps[i]
                }
            } else {
                if (apps[i].traffic!! < app.traffic!!) {
                    app = apps[i]
                }
            }
        }
        return app
    }

    /**
     * Obtiene el uso por hora de una o varias aplicaciones.
     *
     * @param uid - Uid de la aplicación o las aplicaciones
     * @param day - Día que se usará para realizar la búsqueda.
     *
     * @return Una lista de pares con la hora en un Long y el tráfico.
     * */
    suspend fun getAppUsageDayByHour(uid: Int, day: Date): List<Pair<Long, Traffic>> {
        val pairList: ArrayList<Pair<Long, Traffic>> = ArrayList()
        var date = NetworkUsageUtils.getZeroHour(day)



        while (DateUtils.isSameDay(date, day) && date.time <= System.currentTimeMillis()) {
            val start = date.time
            date = DateUtils.setMinutes(date, 59)
            val finish = date.time
            pairList.add(Pair(date.time, getAppUsage(uid, start, finish)))
            date = DateUtils.addHours(date, 1)
            date = DateUtils.setMinutes(date, 0)
        }

        return pairList
    }


    suspend fun getAppUsageByLapsusTime(
        uid: Int,
        start: Long,
        finish: Long,
        myTimeUnit: NetworkUsageUtils.MyTimeUnit
    ): List<Traffic> {

        val traffics = mutableListOf<Traffic>()
        var currentTime = Date(start)

        while (currentTime.time in start..finish) {
            val start1 = currentTime.time
            val finish1: Long

            when (myTimeUnit) {
                NetworkUsageUtils.MyTimeUnit.MONTH -> {
                    currentTime = DateUtils.addMonths(currentTime, 1).also {
                        finish1 = it.time
                    }
                }

                NetworkUsageUtils.MyTimeUnit.DAY -> {
                    currentTime = DateUtils.addDays(currentTime, 1).also {
                        finish1 = it.time
                    }
                }

                NetworkUsageUtils.MyTimeUnit.HOUR -> {
                    currentTime = DateUtils.addHours(currentTime, 1).also {
                        finish1 = it.time
                    }
                }
            }

            traffics.add(
                getAppUsage(uid, start1 + 1, finish1)
            )
        }

        return traffics
    }


    suspend fun updateSimID() {
        simId = simManager.getDefaultSim(SimDelegate.SimType.DATA).id
    }

    companion object {
        const val GENERAL_TRAFFIC_UID = Int.MIN_VALUE
    }
}