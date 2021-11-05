package com.smartsolutions.paquetes.managers

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresApi
import com.smartsolutions.paquetes.annotations.Networks
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.repositories.contracts.ISimRepository
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val EMBEDDED_SIM_ID = "embedded_sim"

class SimManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val simDelegate: SimDelegate,
    private val simRepository: ISimRepository
) : ISimManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val simChangeListener by lazy {

        @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
        object : SubscriptionManager.OnSubscriptionsChangedListener() {

            override fun onSubscriptionsChanged() {

                launch {
                    getInstalledSims()
                }
            }
        }
    }

    override fun isSeveralSimsInstalled(): Boolean {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            return false
        }
        return simDelegate.getActiveSimsInfo().size > 1
    }

    override suspend fun getInstalledSims(relations: Boolean): List<Sim> {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            return listOf(seedEmbeddedSim())
        }
        val sims = mutableListOf<Sim>()

        simDelegate.getActiveSimsInfo().forEach {
            sims.add(
                synchronizeOrCreateSim(it,
                    withContext(Dispatchers.IO) {
                        simRepository.get(simDelegate.getSimId(it), relations)
                    })
            )
        }

        verifyDefaultSim(sims)

        return sims
    }

    override suspend fun getDefaultSim(type: SimDelegate.SimType, relations: Boolean): Sim? {
        val installedSims = getInstalledSims(relations)

        if (installedSims.isEmpty()){
            return null
        }

        if (installedSims.size == 1){
            val sim = installedSims[0].apply {
                defaultData = true
                defaultVoice = true
            }
            setDefaultSim(type, sim)
            return sim
        }

        val defaults = if (type == SimDelegate.SimType.DATA) {
            installedSims.filter { it.defaultData }
        } else {
            installedSims.filter { it.defaultVoice }
        }

        if (defaults.isEmpty() || defaults.size > 1) {
            if (defaults.size > 1)
                resetDefaultValues(type)
            throw IllegalStateException("Default Sim not configured")
        }

        return defaults[0]
    }

    override suspend fun setDefaultSim(type: SimDelegate.SimType, sim: Sim) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return

        resetDefaultValues(type)
        if (type == SimDelegate.SimType.DATA) {
            sim.defaultData = true
        } else {
            sim.defaultVoice = true
        }

        withContext(Dispatchers.IO) {
            simRepository.update(sim)
        }
    }

    override suspend fun getSimBySlotIndex(slotIndex: Int, relations: Boolean): Sim? {
        return getInstalledSims(relations).firstOrNull { it.slotIndex == slotIndex }
    }

    override fun flowInstalledSims(relations: Boolean): Flow<List<Sim>> {
        return simRepository.flow(relations).map { all ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                return@map listOf(seedEmbeddedSim())
            }
            val installed = mutableListOf<Sim>()
            simDelegate.getActiveSimsInfo().forEach { subscription ->
                installed.add(
                    synchronizeOrCreateSim(
                        subscription,
                        all.firstOrNull { it.id == simDelegate.getSimId(subscription) })
                )
            }
            verifyDefaultSim(installed)
            installed
        }
    }

    override fun registerSubscriptionChangedListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && !isRegistered) {
            Handler(Looper.getMainLooper())
                .post {
                    simDelegate.addOnSubscriptionsChangedListener(simChangeListener)
                }
            isRegistered = true
        }
    }

    override fun unregisterSubscriptionChangedListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1 && isRegistered) {
            simDelegate.removeOnSubscriptionsChangedListener(simChangeListener)
            isRegistered = false
        }
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private suspend fun synchronizeOrCreateSim(
        subscriptionInfo: SubscriptionInfo,
        storedSim: Sim?
    ): Sim {
        return if (storedSim != null) {
            storedSim.icon = subscriptionInfo.createIconBitmap(context)
            storedSim.slotIndex = subscriptionInfo.simSlotIndex
            synchronizeSim(storedSim)
            storedSim
        } else {
            buildSim(subscriptionInfo).also {
                withContext(Dispatchers.IO) {
                    simRepository.create(it)
                }
            }
        }
    }

    private suspend fun synchronizeSim(sim: Sim, update: Boolean = true): Sim {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val defaultData =
                simDelegate.getSimId(simDelegate.getActiveSim(SimDelegate.SimType.DATA)) == sim.id
            val defaultVoice =
                simDelegate.getSimId(simDelegate.getActiveSim(SimDelegate.SimType.VOICE)) == sim.id

            if (defaultData != sim.defaultData || defaultVoice != sim.defaultVoice) {
                sim.apply {
                    this.defaultData = defaultData
                    this.defaultVoice = defaultVoice
                }

                if (update) {
                    withContext(Dispatchers.IO) {
                        simRepository.update(sim)
                    }
                }
            }
        }
        return sim
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP_MR1)
    private suspend fun buildSim(subscriptionInfo: SubscriptionInfo): Sim {
        return synchronizeSim(
            Sim(
                simDelegate.getSimId(subscriptionInfo),
                0L,
                Networks.NETWORK_NONE
            ).apply {
                if (subscriptionInfo.number != null && subscriptionInfo.number.isNotBlank()) {
                    phone = subscriptionInfo.number
                }
                icon = subscriptionInfo.createIconBitmap(context)
                slotIndex = subscriptionInfo.simSlotIndex
            }, false
        )
    }

    private suspend fun resetDefaultValues(type: SimDelegate.SimType) {
        val all = withContext(Dispatchers.IO) {
            simRepository.all()
        }.onEach {
            if (type == SimDelegate.SimType.DATA)
                it.defaultData = false
            else
                it.defaultVoice = false
        }

        withContext(Dispatchers.IO) {
            simRepository.update(all)
        }
    }

    private suspend fun seedEmbeddedSim(): Sim {
        return Sim(EMBEDDED_SIM_ID, 0L, Networks.NETWORK_NONE).also {
            if (withContext(Dispatchers.IO) {
                    simRepository.get(EMBEDDED_SIM_ID) == null
                }) {
                withContext(Dispatchers.IO) {
                    simRepository.create(it)
                }
            }
        }
    }

    private suspend fun verifyDefaultSim(sims: List<Sim>){
        if (sims.size == 1 && !sims[0].defaultVoice || !sims[0].defaultData ){
            setDefaultSim(SimDelegate.SimType.DATA, sims[0])
            setDefaultSim(SimDelegate.SimType.VOICE, sims[0])
        }
    }

    companion object {
        private var isRegistered = false
    }
}