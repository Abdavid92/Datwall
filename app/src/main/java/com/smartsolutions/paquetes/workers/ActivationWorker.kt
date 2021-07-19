package com.smartsolutions.paquetes.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result.Failure
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import javax.inject.Inject


class ActivationWorker (
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    @Inject
    lateinit var gson: Gson
    @Inject
    lateinit var client: IRegistrationClient

    init {
        EntryPointAccessors
            .fromApplication(context, ActivationWorkerEntryPoint::class.java)
            .inject(this)
    }

    override fun doWork(): Result {
        return runBlocking {

            try {
                val deviceApp = gson.fromJson(
                    context.dataStore.data.firstOrNull()
                        ?.get(PreferencesKeys.DEVICE_APP),
                    DeviceApp::class.java
                )

                deviceApp.purchased = true
                deviceApp.waitingPurchase = false

                val result = client.updateDeviceApp(deviceApp)

                if (result.isSuccess)
                    return@runBlocking Result.success()
                else {
                    val ex = (result as Failure).throwable

                    if (ex is HttpException && ex.code() == 422)
                        return@runBlocking Result.failure()

                    return@runBlocking Result.retry()
                }

            } catch (e: Exception) {
                return@runBlocking Result.failure()
            }
        }
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ActivationWorkerEntryPoint {
        fun inject(activationWorker: ActivationWorker)
    }
}