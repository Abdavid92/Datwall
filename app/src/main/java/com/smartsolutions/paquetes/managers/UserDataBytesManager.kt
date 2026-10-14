package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.sims.SimType
import com.smartsolutions.paquetes.repositories.contracts.IUsageGeneralRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import javax.inject.Inject
import kotlin.math.abs
import com.smartsolutions.paquetes.repositories.models.DataBytes.DataType
import com.smartsolutions.paquetes.repositories.models.UsageGeneral
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.DateUtils

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val simManager: ISimManager,
    private val usageGeneralRepository: IUsageGeneralRepository
) : IUserDataBytesManager {

    override suspend fun registerTraffic(
        rxBytes: Long,
        txBytes: Long,
        nationalBytes: Long,
        messagingBytes: Long,
        isLte: Boolean
    ) {

        simManager.getDefaultSimBoth(SimType.DATA)?.let { sim ->
            var total = rxBytes + txBytes
            var national = nationalBytes

            if (messagingBytes > 0) {
                withContext(Dispatchers.IO) {
                    return@withContext userDataBytesRepository.bySimId(sim.id)
                }.first { it.type == DataType.MessagingBag }
                    .apply {
                        if (bytes < messagingBytes) {
                            national += (messagingBytes - bytes)
                            bytes = 0L
                        } else {
                            bytes -= messagingBytes
                        }

                        withContext(Dispatchers.IO) {
                            userDataBytesRepository.update(this@apply)
                        }
                    }
            }

            if (national > 0) {
                withContext(Dispatchers.IO) {
                    return@withContext userDataBytesRepository.bySimId(sim.id)
                }.first { it.type == DataType.National }
                    .apply {
                        if (bytes < national) {
                            total += (national - bytes)
                            bytes = 0L
                        } else {
                            bytes -= national
                        }

                        withContext(Dispatchers.IO) {
                            userDataBytesRepository.update(this@apply)
                        }
                    }
            }

            if (isLte) {
                registerLteTraffic(fixTrafficByTime(total), sim.id)
            } else {
                registerTraffic(fixTrafficByTime(total), sim.id)
            }
        }
    }

    override suspend fun synchronizeUserDataBytes(data: List<DataBytes>, simId: String) {
        //Obtengo todos los userDataBytes de la linea
        val userDataBytes = withContext(Dispatchers.IO) {
            return@withContext userDataBytesRepository.bySimId(simId)
        }

        //Iteración por la lista de DataType que me dieron
        data.forEach { dataBytes ->
            //Busco el UserDataBytes correspondiente al DataType en el que estoy
            userDataBytes.first { it.type == dataBytes.type }.apply {
                bytes = dataBytes.bytes
                expiredTime = dataBytes.expiredTime

                if (initialBytes < dataBytes.bytes) {
                    initialBytes = dataBytes.bytes
                }

                // Sin vencimiento todavía: initTimes lo fija con el primer consumo.
                startTime = when {
                    expiredTime == 0L -> 0L
                    type == DataType.DailyBag -> expiredTime - DateUtils.MILLIS_PER_DAY
                    else -> expiredTime - DateUtils.MILLIS_PER_DAY * DataPackages.GENERAL_DURATION
                }
            }
        }

        withContext(Dispatchers.IO) {
            userDataBytesRepository.update(userDataBytes)
        }
    }

    /**
     * Registra el tráfico en la red lte
     */
    private suspend fun registerLteTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        withContext(Dispatchers.IO) {
            return@withContext userDataBytesRepository.bySimId(simId)
        }.filter { /*it.type != DataType.National && it.type != DataType.MessagingBag*/
            DataType.getDataTypeOfLte().contains(it.type)
        }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    consumed = processUserDataBytes(it, consumed)
            }
    }

    /**
     * Registra el tráfico en las redes restantes
     */
    private suspend fun registerTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        withContext(Dispatchers.IO) {
            return@withContext userDataBytesRepository.bySimId(simId)
        }.filter {
            /*it.type == DataType.PromoBonus ||
                    it.type == DataType.International*/
            DataType.getDataTypeOfInternationalNetworks().contains(it.type)
        }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    consumed = processUserDataBytes(it, consumed)
            }
    }

    private suspend fun processUserDataBytes(userDataBytes: UserDataBytes, consumed: Long): Long {

        var consumed1 = consumed

        if (userDataBytes.exists() && !userDataBytes.isExpired()) {
            val rest = userDataBytes.bytes - consumed1

            if (rest < 0) {
                userDataBytes.bytes = 0
                consumed1 = abs(rest)
            } else {
                userDataBytes.bytes = rest
                consumed1 = 0
            }

            initTimes(userDataBytes)
            withContext(Dispatchers.IO) {
                userDataBytesRepository.update(userDataBytes)
                usageGeneralRepository.create(
                    UsageGeneral(
                        System.currentTimeMillis(),
                        userDataBytes.type,
                        consumed,
                        userDataBytes.simId
                    )
                )
            }
        }
        return consumed1
    }

    private fun initTimes(userDataBytes: UserDataBytes) {
        if (userDataBytes.startTime != 0L) {
            return
        }

        userDataBytes.startTime = System.currentTimeMillis()

        if (userDataBytes.type == DataType.DailyBag) {
            userDataBytes.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY
        } else {
            userDataBytes.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY * DataPackages.GENERAL_DURATION
        }
    }

    /**
     * Corrige la cantidad de trafico consumido basado en la hora desde la 1 am hasta las 6 am para usar solo la mitad
     **/
    private fun fixTrafficByTime(bytes: Long): Long {
        return if (DateCalendarUtils.isInDiscountHour(
                System.currentTimeMillis(),
                System.currentTimeMillis()
            )
        ) {
            bytes / 2
        } else {
            bytes
        }
    }
}