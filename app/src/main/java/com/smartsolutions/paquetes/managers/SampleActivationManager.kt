package com.smartsolutions.paquetes.managers

import android.os.Build
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.Device
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result
import kotlinx.coroutines.runBlocking
import java.util.*
import javax.inject.Inject

class SampleActivationManager @Inject constructor(

): IActivationManager {
    override suspend fun canWork(): Pair<Boolean, IActivationManager.ApplicationStatuses> {
        return true to IActivationManager.ApplicationStatuses.Purchased
    }

    override suspend fun isInTrialPeriod(): Boolean {
        return false
    }

    override suspend fun getDevice(): Result<Device> {
        return Result.Success(Device(
            "vjnoir838409",
            "Samsung",
            "SM-A1156M",
            Build.VERSION.SDK_INT
        )
        )
    }

    override suspend fun getDeviceApp(ignoreCache: Boolean): Result<DeviceApp> {
        return Result.Success(getSavedDeviceApp()!!)
    }

    override suspend fun getSavedDeviceApp(): DeviceApp? {
        return DeviceApp(
            "vdopp09we0f8",
            true,
            false,
            false,
            Date(),
            null,
            null,
            false,
            "vjnoir838409",
            "com.smartsolutions.paquetes",
            Date()
        )
    }

    override fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener) {
        listener.onPurchased(runBlocking {
            getSavedDeviceApp()!!
        })
    }

    override suspend fun beginActivation(deviceApp: DeviceApp): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun transferCreditByUSSD(key: String, deviceApp: DeviceApp): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun confirmPurchase(
        smsBody: String,
        phone: String,
        simIndex: Int
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun isWaitingPurchased(): Boolean {
        return false
    }
}