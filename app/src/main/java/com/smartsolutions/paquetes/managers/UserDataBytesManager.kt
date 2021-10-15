package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackages
import com.smartsolutions.paquetes.helpers.NetworkUsageUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
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
import org.apache.commons.lang.time.DateUtils

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val simManager: ISimManager,
    private val usageGeneralRepository: IUsageGeneralRepository
) : IUserDataBytesManager {

    override suspend fun addDataBytes(dataPackage: DataPackage, simId: String) {
        if (dataPackage.id == DataPackages.PackageId.DailyBag) {
            withContext(Dispatchers.IO){
                userDataBytesRepository.get(simId, DataType.DailyBag)
            } .apply {
                bytes += dataPackage.bytesLte
                initialBytes = bytes
                startTime = 0
                expiredTime = 0

                withContext(Dispatchers.IO) {
                    userDataBytesRepository.update(this@apply)
                }
            }


        } else {
            val userDataBytes = withContext(Dispatchers.IO){
                userDataBytesRepository.bySimId(simId)
            }.apply {

                val international = first { it.type == DataType.International }
                international.bytes += dataPackage.bytes
                international.initialBytes = international.bytes
                international.startTime = 0
                international.expiredTime = 0

                val internationalLte = first { it.type == DataType.InternationalLte }
                internationalLte.bytes += dataPackage.bytesLte
                internationalLte.initialBytes = internationalLte.bytes
                internationalLte.startTime = 0
                internationalLte.expiredTime = 0

                val national = first { it.type == DataType.National }
                national.bytes = dataPackage.nationalBytes
                national.initialBytes = dataPackage.nationalBytes
                national.startTime = 0
                national.expiredTime = 0
            }

            withContext(Dispatchers.IO){
                userDataBytesRepository.update(userDataBytes)
            }
        }
    }

    override suspend fun addPromoBonus(simId: String, bytes: Long) {
        val userDataBytes = withContext(Dispatchers.IO) {
            userDataBytesRepository.bySimId(simId)
        }.filter { it.type != DataType.DailyBag }.onEach {

            if (it.type == DataType.PromoBonus) {
                it.bytes += bytes
                it.initialBytes = it.bytes
                it.startTime = System.currentTimeMillis()
            }
            //Le ampliamos la fecha de expiración a todos UserDataBytes.
            it.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY * 30
        }

        withContext(Dispatchers.IO) {
            userDataBytesRepository.update(userDataBytes)
        }
    }

    override suspend fun registerTraffic(
        rxBytes: Long,
        txBytes: Long,
        nationalBytes: Long,
        isLte: Boolean
    ) {

        val sim = simManager.getDefaultSim(SimDelegate.SimType.DATA)
        var total = rxBytes + txBytes

        if (nationalBytes > 0) {
            withContext(Dispatchers.IO) {
                return@withContext userDataBytesRepository.bySimId(sim.id)
            }.first { it.type == DataType.National }
                .apply {
                    if (bytes < nationalBytes) {
                        total += (nationalBytes - bytes)
                        bytes = 0L
                    } else {
                        bytes -= nationalBytes
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

                startTime = if (type == DataType.DailyBag)
                    expiredTime - DateUtils.MILLIS_PER_DAY
                else
                    expiredTime - DateUtils.MILLIS_PER_DAY * 30
            }
        }

        withContext(Dispatchers.IO) {
            userDataBytesRepository.update(userDataBytes)
        }
    }

    private suspend fun registerLteTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        withContext(Dispatchers.IO) {
            return@withContext userDataBytesRepository.bySimId(simId)
        }.filter { it.type != DataType.National }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    consumed = processUserDataBytes(it, consumed)
            }
    }


    private suspend fun registerTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        withContext(Dispatchers.IO) {
            return@withContext userDataBytesRepository.bySimId(simId)
        }.filter {
            it.type == DataType.PromoBonus ||
                    it.type == DataType.International
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
            userDataBytes.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY * 30
        }
    }

    /**
     * Corrige la cantidad de trafico consumido basado en la hora desde la 1 am hasta las 6 am para usar solo la mitad
     **/
    private fun fixTrafficByTime(bytes: Long): Long {
        return if (NetworkUsageUtils.isInDiscountHour(
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