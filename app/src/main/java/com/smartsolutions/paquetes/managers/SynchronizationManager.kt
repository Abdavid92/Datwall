package com.smartsolutions.paquetes.managers

import android.content.Context
import androidx.datastore.preferences.core.edit
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.dataStore
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.contracts.IDataPackageManager
import com.smartsolutions.paquetes.managers.contracts.IMiCubacelManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.managers.contracts.IUserDataBytesManager
import com.smartsolutions.paquetes.micubacel.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.Sim
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.apache.commons.lang3.StringUtils
import javax.inject.Inject

class SynchronizationManager @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val miCubacelManager: IMiCubacelManager,
    private val userDataBytesManager: IUserDataBytesManager,
    private val ussdHelper: USSDHelper
) : ISynchronizationManager {

    private var _synchronizationMode = IDataPackageManager.ConnectionMode.USSD
    override var synchronizationMode: IDataPackageManager.ConnectionMode
        get() = _synchronizationMode
        set(value) {
            if (value == IDataPackageManager.ConnectionMode.Unknown)
                return

            GlobalScope.launch(Dispatchers.IO) {
                context.dataStore.edit {
                    it[PreferencesKeys.SYNCHRONIZATION_MODE] = value.name
                }
            }
            _synchronizationMode = value
        }

    init {
        GlobalScope.launch(Dispatchers.IO) {
            context.dataStore.data.collect {
                _synchronizationMode = IDataPackageManager.ConnectionMode
                    .valueOf(it[PreferencesKeys.SYNCHRONIZATION_MODE] ?: _synchronizationMode.name)
            }
        }
    }

    override suspend fun synchronizeUserDataBytes(sim: Sim) {
        if (_synchronizationMode == IDataPackageManager.ConnectionMode.MiCubacel) {
            if (sim.miCubacelAccount == null)
                throw NoSuchElementException()

            val data = miCubacelManager.synchronizeUserDataBytes(sim.miCubacelAccount)
                .getOrThrow()

            userDataBytesManager.synchronizeUserDataBytes(fillMissingDataBytes(data), sim.id)
        } else if (_synchronizationMode == IDataPackageManager.ConnectionMode.USSD) {
            val data = mutableListOf<DataBytes>()

            val bytesPackages = ussdHelper.sendUSSDRequest("*222*328#")
            val bonusPackages = ussdHelper.sendUSSDRequest("*222*266#")

            data.addAll(obtainDataBytesPackages(bytesPackages))
            data.addAll(obtainDataByteBonus(bonusPackages))

            if (data.isNotEmpty())
                userDataBytesManager.synchronizeUserDataBytes(fillMissingDataBytes(data), sim.id)
        }
    }

    override fun scheduleUserDataBytesSynchronization(intervalInMinutes: Int) {
        TODO("Not yet implemented")
    }

    private fun obtainDataBytesPackages(bytesPackages: Array<CharSequence>): Collection<DataBytes> {
        var response = StringUtils.join(bytesPackages)

        val data = mutableListOf<DataBytes>()

        //Cosa gorda


        return data
    }

    private fun obtainDataByteBonus(bonusPackages: Array<CharSequence>): Collection<DataBytes> {
        val response = StringUtils.join(bonusPackages)

        val data = mutableListOf<DataBytes>()

        //Cosa goooordaaaa

        return data
    }

    private fun fillMissingDataBytes(data: List<DataBytes>): List<DataBytes> {
        val list = data.toMutableList()

        DataBytes.DataType.values().forEach { type ->
            if (list.firstOrNull { it.type == type } == null) {
                list.add(DataBytes(type, 0, 0))
            }
        }
        return list
    }
}