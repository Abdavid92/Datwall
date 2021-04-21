package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.managers.models.Traffic
import com.smartsolutions.paquetes.repositories.models.App
import com.smartsolutions.paquetes.repositories.models.IApp
import org.apache.commons.lang.time.DateUtils
import java.util.*

/**
 * Administrador de estadísticas de tráfico de datos de la aplicaciones.
 * Se encarga de recopilar y obtener el uso de datos de las aplicaciones.
 * */
abstract class NetworkUsageManager {

    /**
     * Obtiene el tráfico de un tiempo dado de una o varias aplicaciones por el uid.
     *
     * @param uid - Uid de la aplicación o  las aplicaciones.
     * @param start - Tiempo de inicio del tráfico de datos que se quiere obtener.
     * @param finish - Tiempo de finalización del tráfico de datos.
     *
     * @return Una instancia de Traffic con los datos recopilados.
     * */
    abstract suspend fun getAppUsage(uid : Int, start: Long, finish: Long): Traffic

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
     * Obtiene el tráfico de una lista de aplicaciones y  se lo asigna a estas.
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
    abstract suspend fun getUsageTotal(start : Long, finish : Long) : Traffic

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
    suspend fun getAppPerConsumed(apps: List<App>, start: Long, finish: Long, moreConsumed : Boolean) : App?{
        if (apps.isEmpty()){
            return null
        }
        fillAppsUsage(apps, start, finish)
        var app = apps[0]
        for (i in 1 .. apps.size ){
            if (moreConsumed) {
                if (apps[i].traffic!! > app.traffic!!) {
                    app = apps[i]
                }
            }else {
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
     * @param uid - Uid de la aplicación o  las aplicaciones
     * @param day - Día que se usará para realizar la búsqueda.
     *
     * @return Una lista de pares con la hora en un Long y el tráfico.
     * */
    suspend fun getAppUsageDayByHour(uid: Int, day : Date) : List<Pair<Long, Traffic>>{
        val pairList: ArrayList<Pair<Long, Traffic>> = ArrayList()
        var date = NetworkUtils.getZeroHour(day)

        while (DateUtils.isSameDay(date, day) && date.time <= System.currentTimeMillis()){
            val start = date.time
            date = DateUtils.setMinutes(date, 59)
            val finish = date.time
            pairList.add(Pair(date.time, getAppUsage(uid, start, finish)))
            date = DateUtils.addHours(date, 1)
            date = DateUtils.setMinutes(date, 0)
        }

        return pairList
    }
}