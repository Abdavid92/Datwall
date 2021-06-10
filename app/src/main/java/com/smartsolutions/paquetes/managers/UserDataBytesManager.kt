package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.micubacel.models.DataBytes
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject
import kotlin.math.abs
import com.smartsolutions.paquetes.micubacel.models.DataBytes.DataType

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val simManager: SimManager
) : IUserDataBytesManager {

    override suspend fun addDataBytes(dataPackage: DataPackage, simId: String) {
        if (dataPackage.id == DataPackagesContract.DailyBag.id) {
            userDataBytesRepository.get(simId, DataType.DailyBag)
                .apply {
                    bytes += dataPackage.bytesLte
                    initialBytes = bytes
                    startTime = 0
                    expiredTime = 0

                    userDataBytesRepository.update(this)
                }
        } else {
            userDataBytesRepository.update(userDataBytesRepository.bySimId(simId).apply {
                val bonus = first { it.type == DataType.Bonus }
                bonus.bytes += dataPackage.bonusBytes
                bonus.initialBytes = bonus.bytes
                bonus.startTime = 0
                bonus.expiredTime = 0

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

                val national = first { it.type == DataType.National}
                national.bytes = dataPackage.nationalBytes
                national.initialBytes = dataPackage.nationalBytes
                national.startTime = 0
                national.expiredTime = 0
            })
        }
    }

    override suspend fun addPromoBonus(simId: String) {
        userDataBytesRepository.update(userDataBytesRepository.bySimId(simId)
            .filter { it.type != DataType.DailyBag }.onEach {

                if (it.type == DataType.PromoBonus) {
                    it.bytes += DataUnitBytes.GB.toLong()
                    it.initialBytes = it.bytes
                }

                it.startTime = System.currentTimeMillis()
                it.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY * 30
            })
    }

    override suspend fun registerTraffic(rxBytes: Long, txBytes: Long, isLte: Boolean) {

        //TODO: Procesar los bytes nacionales
        val sim = simManager.getDefaultDataSim()

        if (isLte) {
            registerLteTraffic(fixTrafficByTime(rxBytes + txBytes), sim.id)
        } else {
            registerTraffic(fixTrafficByTime(rxBytes + txBytes), sim.id)
        }
    }

    override suspend fun synchronizeUserDataBytes(data: List<DataBytes>, simId: String) {
        //Obtengo todos los userDataBytes de la linea
        val userDataBytes = userDataBytesRepository.bySimId(simId)

        //Iteración por la lista de DataType que me dieron
        data.forEach { dataBytes ->
            //Busco el UserDataBytes correspondiente al DataType en el que estoy
            userDataBytes.first { it.type == dataBytes.type }.apply {
                bytes = dataBytes.bytes
                expiredTime = dataBytes.expiredTime

                startTime = if (type == DataType.DailyBag)
                    expiredTime - DateUtils.MILLIS_PER_DAY
                else
                    expiredTime - DateUtils.MILLIS_PER_DAY * 30
            }
        }
    }

    private suspend fun registerLteTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        userDataBytesRepository.bySimId(simId)
            .filter { it.type != DataType.National }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    consumed = processUserDataBytes(it, consumed)
            }
    }


    private suspend fun registerTraffic(bytes: Long, simId: String) {
        var consumed = bytes

        userDataBytesRepository.bySimId(simId)
            .filter {
                it.type == DataType.PromoBonus ||
                        it.type == DataType.International }
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
            userDataBytesRepository.update(userDataBytes)
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
     *
     **/
    private fun fixTrafficByTime (bytes: Long) : Long {
        return if (NetworkUtils.isInDiscountHour(System.currentTimeMillis(), System.currentTimeMillis())){
            bytes / 2
        }else {
            bytes
        }
    }
}