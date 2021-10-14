package com.smartsolutions.paquetes.managers

import android.os.Build
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.managers.contracts.IActivationManager2
import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

class SampleActivationManager @Inject constructor(

): IActivationManager2 {
    override suspend fun canWork(): Pair<Boolean, IActivationManager2.ApplicationStatuses> {
        TODO("Not yet implemented")
    }

    override suspend fun isInTrialPeriod(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getApplicationStatus(listener: IActivationManager2.ApplicationStatusListener) {
        TODO("Not yet implemented")
    }

    override suspend fun transferCreditByUSSD(key: String, license: License): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun confirmPurchase(
        smsBody: String,
        phone: String,
        simIndex: Int
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun isWaitingPurchased(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun getLicense(): Result<License> {
        TODO("Not yet implemented")
    }

    override suspend fun getLocalLicense(): License? {
        TODO("Not yet implemented")
    }

}