package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.smartsolutions.paquetes.BuildConfig
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.annotations.ApplicationStatus
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.models.AndroidApp
import com.smartsolutions.paquetes.serverApis.models.License
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.*
import javax.inject.Inject

class SampleActivationManager @Inject constructor(
    @ApplicationContext
    context: Context
): IActivationManager {

    private val license = License(
        "vskhbfvski",
        true,
        false,
        Build.MANUFACTURER,
        Build.MODEL,
        null,
        null,
        Date(),
        Date(),
        context.packageName
    ).apply {
        androidApp = AndroidApp(
            1,
            context.getString(R.string.app_name),
            context.packageName,
            18,
            31,
            BuildConfig.VERSION_NAME,
            AndroidApp.UpdatePriority.Low,
            "",
            ApplicationStatus.ACTIVATED,
            true,
            7,
            30,
            "87462374673295092934",
            "",
            "",
            ""
        )
    }

    private val _onConfrimPurchase = MutableLiveData<Result<Unit>>()

    override val onConfirmPurchase: LiveData<Result<Unit>>
        get() = _onConfrimPurchase

    override suspend fun canWork(): Pair<Boolean, IActivationManager.ApplicationStatuses> {
        return false to IActivationManager.ApplicationStatuses.TrialPeriod
    }

    override suspend fun isInTrialPeriod(): Boolean {
        return false
    }

    override fun getApplicationStatus(listener: IActivationManager.ApplicationStatusListener) {
        //listener.onPurchased(license)
        listener.onTrialPeriod(license.apply {
            isPurchased = false
            isRestored = false
        }, false)
    }

    override suspend fun transferCreditByUSSD(key: String, license: License): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun confirmPurchase(
        smsBody: String,
        phone: String,
        simIndex: Int
    ): Result<Unit> {
        _onConfrimPurchase.postValue(Result.Success(Unit))
        return Result.Success(Unit)
    }

    override suspend fun getLicense(): Result<License> {
        return Result.Success(license)
    }

    override suspend fun getLocalLicense(): License {
        return license
    }

}