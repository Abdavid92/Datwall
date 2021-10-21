package com.smartsolutions.paquetes.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.internalDataStore
import com.smartsolutions.paquetes.settingsDataStore
import com.smartsolutions.paquetes.managers.ActivationManager
import com.smartsolutions.paquetes.serverApis.contracts.IActivationClient
import com.smartsolutions.paquetes.serverApis.models.License
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
    private val client: IActivationClient
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val data = applicationContext.internalDataStore.data.firstOrNull()
                ?.get(PreferencesKeys.LICENSE) ?: return Result.failure()

            val license = gson.fromJson(
                ActivationManager.decrypt(data),
                License::class.java
            )

            if (license.isPurchased){
                license.isRestored = true
            }

            license.isPurchased = true

            val result = client.updateLicense(license)

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