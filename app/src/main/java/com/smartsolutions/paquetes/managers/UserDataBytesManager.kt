package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.helpers.NetworkUtil
import com.smartsolutions.paquetes.helpers.SimsHelper
import com.smartsolutions.paquetes.helpers.createDataPackageId
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.math.abs
import kotlin.math.absoluteValue

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val statisticsManager: StatisticsManager,
    private val networkUtil: NetworkUtil,
    private val simsHelper: SimsHelper
): IUserDataBytesManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override suspend fun addDataBytes(dataPackage: DataPackage, simIndex: Int) {
        TODO("Incomplete")
        /*if (dataPackage.id == createDataPackageId(
                DataPackagesContract.DailyBag.name,
                DataPackagesContract.DailyBag.price)) {

            val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

            bagDaily.bytesLte = statisticsManager.remainingBagDaily(simIndex).bytes + dataPackage.bytesLte
            bagDaily.initialBytes = bagDaily.bytesLte
            bagDaily.startTime = System.currentTimeMillis()
            bagDaily.expiredTime = 0
            userDataBytesRepository.update(bagDaily)

        } else {
            val data = userDataBytesRepository.getBySimIndex(simIndex)

        }*/
    }

    override suspend fun registerTraffic(rxBytes: Long, txBytes: Long) {
        val isLte = networkUtil.getNetworkGeneration() == NetworkUtil.NetworkType.NETWORK_4G

        val simIndex = simsHelper.getActiveDataSimIndex()

        if (isLte) {
            registerLteTraffic(rxBytes + txBytes, simIndex)
        } else {
            registerTraffic(rxBytes + txBytes, simIndex)
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

                userDataBytesRepository.update(userDataBytes)
            }
        }

        userDataBytesRepository.getAllByPriority(simIndex)
            .filter { it.type != UserDataBytes.DataType.National }
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

                userDataBytesRepository.update(userDataBytes)
            }
        }

        userDataBytesRepository.getAllByPriority(simIndex)
            .filter {
                it.type == UserDataBytes.DataType.PromoBonus ||
                        it.type == UserDataBytes.DataType.International
            }.forEach {
                if (consumed > 0)
                    processUserDataBytes(it)
            }
    }
}