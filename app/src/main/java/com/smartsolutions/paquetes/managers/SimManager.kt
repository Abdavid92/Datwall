package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import android.telephony.SubscriptionInfo
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.annotations.Networks.Companion.NETWORK_NONE
import com.smartsolutions.paquetes.exceptions.MissingPermissionException
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import com.smartsolutions.paquetes.helpers.SimDelegate.SimType
import kotlin.jvm.Throws

/**
 * Administra las lineas instaladas en el dispositivo.
 * */
class SimManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) {

    /**
     * Id de la linea embeida para android 5.0(api 21).
     * */
    private val embeddedSimId = "legacy_sim"

    /**
     * Obtiene la linea predeterminada para llamadas. Si la versión de
     * las apis android es 24 o mayor obtendrá la predeterminada del sistema.
     * De lo contrario obtendrá la que se estableció como predeterminada manualmente
     * mediante el método [setDefaultVoiceSim]. Si es android 23 o 22 y no existe ninguna
     * linea establecida como predeterminada se lanza un [IllegalStateException].
     *
     * @return [Sim]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getDefaultVoiceSim(): Sim {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val subscriptionInfo = simDelegate.getActiveSim(SimType.VOICE)

            return findSim(subscriptionInfo)
        }
        try {
            return getInstalledSims().first { it.defaultVoice }
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("Default voice sim not configured")
        }
    }

    /**
     * Establece la linea predeterminada para las llamadas. Esto se usa
     * solamente en las apis 23 y 22 ya que no se pueden obtener
     * mediante el sistema.
     *
     * @param sim - Linea que se establecerá como predeterminada.
     * */
    suspend fun setDefaultVoiceSim(sim: Sim) {
        resetDefault(SimType.VOICE)
        sim.defaultVoice = true
        simRepository.update(sim)
    }

    /**
     * Obtiene la linea predeterminada para los datos. Si la versión de
     * las apis android es 24 o mayor obtendrá la predeterminada del sistema.
     * De lo contrario obtendrá la que se estableció como predeterminada manualmente
     * mediante el método [setDefaultDataSim]. Si es android 23 o 22 y no existe ninguna
     * linea establecida como predeterminada se lanza un [IllegalStateException].
     *
     * @return [Sim]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getDefaultDataSim(): Sim {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val subscriptionInfo = simDelegate.getActiveSim(SimType.DATA)

            return findSim(subscriptionInfo)
        }
        try {
            return getInstalledSims().first { it.defaultData }
        } catch (e: NoSuchElementException) {
            throw IllegalStateException("Default data sim not configured")
        }
    }

    /**
     * Establece la linea predeterminada para los datos. Esto se usa
     * solamente en las apis 23 y 22 ya que no se pueden obtener
     * mediante el sistema.
     *
     * @param sim - Linea que se establecerá como predeterminada.
     * */
    suspend fun setDefaultDataSim(sim: Sim) {
        resetDefault(SimType.DATA)
        sim.defaultData = true
        simRepository.update(sim)
    }

    /**
     * Indica si hay más de una linea instalada.
     * */
    @Throws(MissingPermissionException::class)
    suspend fun isInstalledSeveralSims(): Boolean =
        getInstalledSims().size > 1

    /**
     * Obtiene todas las lienas instaladas.
     *
     * @return [List]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getInstalledSims(): List<Sim> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            val list = mutableListOf<Sim>()

            simDelegate.getActiveSimsInfo().forEach {
                list.add(findSim(it))
            }

            return list
        }
        return listOf(seedEmbeddedSim())
    }

    /**
     * Obtiene una linea por el índice.
     *
     * @param simIndex - Índice en base a 1 de la linea. Normalmente este es el
     * slot donde está instalada.
     *
     * @return [Sim]
     * */
    @Throws(MissingPermissionException::class)
    suspend fun getSimByIndex(simIndex: Int): Sim {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            return findSim(simDelegate.getSimByIndex(simIndex))
        }
        return seedEmbeddedSim()
    }

    /**
     * Siembra la linea embeida para android 21 y posteriormente la retorna.
     * En caso existir en base de datos, solo la retorna. Esto se hace porque
     * esta api de android no tiene soporte para varias lineas. Por lo tanto,
     * hay que crear una linea predeterminada para cumplir con las restricciones
     * foráneas de la base de datos.
     *
     * @return [Sim]
     * */
    private suspend fun seedEmbeddedSim(): Sim {
        simRepository.get(embeddedSimId)?.let {
            return it
        }

        val sim = Sim(embeddedSimId, 0, NETWORK_NONE)
        sim.defaultVoice = true
        sim.defaultData = true

        simRepository.create(sim)

        return sim
    }

    /**
     * Busca una linea en base de datos. En caso de no existir la crea.
     *
     * @param subscriptionInfo - Información para buscar o crear la linea.
     *
     * @return [Sim]
     * */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private suspend fun findSim(subscriptionInfo: SubscriptionInfo): Sim {
        val id = simDelegate.getSimId(subscriptionInfo)

        simRepository.get(id)?.let {
            it.icon = subscriptionInfo.createIconBitmap(context)

            return it
        }

        val sim = Sim(id, 0, NETWORK_NONE)

        val phone = subscriptionInfo.number

        if (phone.isNotBlank())
            sim.phone = phone

        simRepository.create(sim)

        sim.icon = subscriptionInfo.createIconBitmap(context)

        return sim
    }

    /**
     * Resetea los valores [Sim.defaultVoice] o [Sim.defaultData] de todas las
     * lineas en dependencia del [SimType] dado.
     * */
    private suspend fun resetDefault(type: SimType) {
        simRepository.update(simRepository.all().onEach {
            when (type) {
                SimType.VOICE -> it.defaultVoice = false
                SimType.DATA -> it.defaultData = false
            }
        })
    }
}