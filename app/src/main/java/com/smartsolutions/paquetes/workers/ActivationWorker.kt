package com.smartsolutions.paquetes.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result.Failure
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.HttpException

class ActivationWorker @AssistedInject constructor(
    @Assisted
    context: Context,
    @Assisted
    params: WorkerParameters,
    private val gson: Gson,
    private val client: IRegistrationClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val deviceApp = gson.fromJson(
                applicationContext.dataStore.data.firstOrNull()
                    ?.get(PreferencesKeys.DEVICE_APP),
                DeviceApp::class.java
            )

            deviceApp.purchased = true
            deviceApp.waitingPurchase = false

            val result = client.updateDeviceApp(deviceApp)

            if (result.isSuccess)
                Result.success()
            else {
                val ex = (result as Failure).throwable

                if (ex is HttpException && ex.code() == 422)
                    Result.failure()

                Result.retry()
            }

        } catch (e: Exception) {
            Result.failure()
        }
    }
}