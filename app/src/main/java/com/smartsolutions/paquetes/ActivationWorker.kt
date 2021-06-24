package com.smartsolutions.paquetes

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.gson.Gson
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import com.smartsolutions.paquetes.serverApis.contracts.IRegistrationClient
import com.smartsolutions.paquetes.serverApis.models.DeviceApp
import com.smartsolutions.paquetes.serverApis.models.Result
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException


class ActivationWorker (
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val gson: Gson
    private val client: IRegistrationClient

    init {
        val entryPoint = EntryPointAccessors
            .fromApplication(context, ActivationWorkerEntryPoint::class.java)

        gson = entryPoint.getGson()
        client = entryPoint.getRegistrationClient()
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
                    val ex = (result as com.smartsolutions.paquetes.serverApis.models.Result.Failure).throwable

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
        fun getGson(): Gson
        fun getRegistrationClient(): IRegistrationClient
    }
}