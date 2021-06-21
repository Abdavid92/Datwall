package com.smartsolutions.paquetes

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.smartsolutions.paquetes.managers.contracts.IActivationManager
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent


class ActivationWorker (
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    private val activationManager: IActivationManager

    init {
        val entryPoint = EntryPointAccessors
            .fromApplication(context, ActivationWorkerEntryPoint::class.java)

        activationManager = entryPoint.getActivationManager()
    }

    override fun doWork(): Result {
        TODO("Not yet implemented")
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ActivationWorkerEntryPoint {
        fun getActivationManager(): IActivationManager
    }
}