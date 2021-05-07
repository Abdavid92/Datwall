package com.smartsolutions.paquetes.managers

import com.smartsolutions.paquetes.data.DataPackagesContract
import com.smartsolutions.paquetes.helpers.createDataPackageId
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataPackage
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.*
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class UserDataBytesManager @Inject constructor(
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val statisticsManager: StatisticsManager
): IUserDataBytesManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

    override suspend fun addDataBytes(dataPackage: DataPackage, simIndex: Int) {
        if (dataPackage.id == createDataPackageId(
                DataPackagesContract.DailyBag.name,
                DataPackagesContract.DailyBag.price)) {

            val bagDaily = userDataBytesRepository.byType(UserDataBytes.DataType.BagDaily, simIndex)

            bagDaily.bytesLte = statisticsManager.remainingBagDaily(simIndex).bytes + dataPackage.bytesLte
            bagDaily.startTime = System.currentTimeMillis()
            bagDaily.expiredTime = 0
            userDataBytesRepository.update(bagDaily)

        } else {
            val data = userDataBytesRepository.getBySimIndex(simIndex)

        }
    }
}