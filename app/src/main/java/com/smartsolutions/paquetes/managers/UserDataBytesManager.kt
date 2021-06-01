package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackagesContract.DailyBag
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.helpers.SimsHelper
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import org.apache.commons.lang.time.DateUtils
import javax.inject.Inject
import kotlin.math.abs

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val networkUtil: NetworkUtil,
    private val simsHelper: SimsHelper
): IUserDataBytesManager {

    override suspend fun addDataBytes(dataPackage: DataPackage, simIndex: Int) {
        if (dataPackage.id == DailyBag.id) {
           userDataBytesRepository.byType(UserDataBytes.DataType.DailyBag, simIndex).apply {
               bytesLte += dataPackage.bytesLte
               initialBytes = bytesLte
               startTime = 0L
               expiredTime = 0L
               userDataBytesRepository.update(this)
           }
        } else {
            userDataBytesRepository.update(userDataBytesRepository.all(simIndex).apply {
                val bonus = first { it.type == UserDataBytes.DataType.Bonus }
                bonus.bytesLte += dataPackage.bonusBytes
                bonus.initialBytes = bonus.bytesLte
                bonus.startTime = 0L
                bonus.expiredTime = 0L

                val international = first { it.type == UserDataBytes.DataType.International }
                international.bytes += dataPackage.bytes
                international.bytesLte += dataPackage.bytesLte
                international.initialBytes = international.bytes + international.bytesLte
                international.startTime = 0L
                international.expiredTime = 0L

                val national = first { it.type == UserDataBytes.DataType.National}
                national.bytes = dataPackage.nationalBytes
                national.initialBytes = dataPackage.nationalBytes
                national.startTime = 0L
                national.expiredTime = 0L
            })
        }
    }

    override suspend fun registerTraffic(rxBytes: Long, txBytes: Long) {
        val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G

        val simIndex = simsHelper.getActiveDataSimIndex()

        if (isLte) {
            registerLteTraffic(fixTrafficByTime(rxBytes + txBytes), simIndex)
        } else {
            registerTraffic(fixTrafficByTime(rxBytes + txBytes), simIndex)
        }
    }

    private suspend fun registerLteTraffic(bytes: Long, simIndex: Int) {
        var consumed = bytes

        suspend fun processUserDataBytes(userDataBytes: UserDataBytes) {
            if (userDataBytes.exists() && !userDataBytes.isExpired()) {
                var rest = userDataBytes.bytesLte - consumed

                if (rest < 0) {
                    userDataBytes.bytesLte = 0
                    consumed = abs(rest)
                } else {
                    userDataBytes.bytesLte = rest
                    consumed = 0
                }

                if (consumed > 0) {
                    rest = userDataBytes.bytes - consumed

                    if (rest < 0) {
                        userDataBytes.bytes = 0
                        consumed = abs(rest)
                    } else {
                        userDataBytes.bytes = rest
                        consumed = 0
                    }
                }

                initTimes(userDataBytes)
                userDataBytesRepository.update(userDataBytes)
            }
        }

        userDataBytesRepository.all(simIndex)
            .filter { it.type != UserDataBytes.DataType.National }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    processUserDataBytes(it)
            }
    }


    private suspend fun registerTraffic(bytes: Long, simIndex: Int) {
        var consumed = bytes

        suspend fun processUserDataBytes(userDataBytes: UserDataBytes) {
            if (userDataBytes.exists() && !userDataBytes.isExpired()) {
                val rest = userDataBytes.bytes - consumed

                if (rest < 0) {
                    userDataBytes.bytes = 0
                    consumed = abs(rest)
                } else {
                    userDataBytes.bytes = rest
                    consumed = 0
                }

                initTimes(userDataBytes)
                userDataBytesRepository.update(userDataBytes)
            }
        }

        userDataBytesRepository.all(simIndex)
            .filter {
                it.type == UserDataBytes.DataType.PromoBonus ||
                        it.type == UserDataBytes.DataType.International }
            .sortedBy { it.priority }
            .forEach {
                if (consumed > 0)
                    processUserDataBytes(it)
            }
    }

    private fun initTimes(userDataBytes: UserDataBytes) {
        if (userDataBytes.startTime == 0L) {
           return
        }

        userDataBytes.startTime = System.currentTimeMillis()

        if (userDataBytes.type == UserDataBytes.DataType.DailyBag) {
            userDataBytes.expiredTime = System.currentTimeMillis() + DateUtils.MILLIS_PER_DAY
        }else {
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