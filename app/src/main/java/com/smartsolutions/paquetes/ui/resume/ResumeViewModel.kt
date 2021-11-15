package com.smartsolutions.paquetes.ui.resume

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.*
import com.smartsolutions.paquetes.PreferencesKeys
import com.smartsolutions.paquetes.exceptions.USSDRequestException
import com.smartsolutions.paquetes.helpers.DateCalendarUtils
import com.smartsolutions.paquetes.helpers.SimDelegate
import com.smartsolutions.paquetes.helpers.USSDHelper
import com.smartsolutions.paquetes.managers.StatisticsManager
import com.smartsolutions.paquetes.managers.contracts.ISimManager
import com.smartsolutions.paquetes.managers.contracts.IStatisticsManager
import com.smartsolutions.paquetes.managers.contracts.ISynchronizationManager
import com.smartsolutions.paquetes.managers.models.DataUnitBytes
import com.smartsolutions.paquetes.managers.models.DataUnitBytes.DataValue
import com.smartsolutions.paquetes.repositories.contracts.IUserDataBytesRepository
import com.smartsolutions.paquetes.repositories.models.DataBytes
import com.smartsolutions.paquetes.repositories.models.Sim
import com.smartsolutions.paquetes.repositories.models.UserDataBytes
import com.smartsolutions.paquetes.uiDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.lang.time.DateUtils
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class ResumeViewModel @Inject constructor(
    application: Application,
    private val simManager: ISimManager,
    private val userDataBytesRepository: IUserDataBytesRepository,
    private val synchronizationManager: ISynchronizationManager,
    private val statisticsManager: IStatisticsManager
) : AndroidViewModel(application) {

    private var filter = FilterUserDataBytes.NORMAL
    private var liveUserDataBytes =
        MutableLiveData<Pair<List<UserDataBytes>, Triple<Int, DataValue, DataValue>>>()
    private var userDataBytes = emptyList<UserDataBytes>()
    private var liveAverages = MutableLiveData<Pair<DataUnitBytes, DataUnitBytes>>()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.data.collect {
                filter = FilterUserDataBytes.valueOf(
                    it[PreferencesKeys.RESUME_FILTER] ?: FilterUserDataBytes.NORMAL.name
                )
                withContext(Dispatchers.Default) {
                    if (userDataBytes.isNotEmpty()) {
                        liveUserDataBytes.postValue(
                            filter(userDataBytes) to calculateTotals(
                                userDataBytes
                            )
                        )
                    }
                }
            }
        }
    }


    fun setFilter(filterUserDataBytes: FilterUserDataBytes) {
        viewModelScope.launch(Dispatchers.IO) {
            getApplication<Application>().uiDataStore.edit {
                it[PreferencesKeys.RESUME_FILTER] = filterUserDataBytes.name
            }
        }
    }


    fun getInstalledSims(): LiveData<List<Sim>> {
        return simManager.flowInstalledSims().asLiveData(Dispatchers.IO)
    }

    fun getUserDataBytes(simId: String): LiveData<Pair<List<UserDataBytes>, Triple<Int, DataValue, DataValue>>> {
        viewModelScope.launch(Dispatchers.IO) {
            userDataBytesRepository.flowBySimId(simId).collect { userData ->
                userDataBytes = userData
                withContext(Dispatchers.Default) {
                    obtainAverages(userData)
                    liveUserDataBytes.postValue(filter(userData) to calculateTotals(userData))
                }
            }
        }
        return liveUserDataBytes
    }

    fun getAverages(): LiveData<Pair<DataUnitBytes, DataUnitBytes>>{
        return liveAverages
    }



    private fun calculateTotals(list: List<UserDataBytes>): Triple<Int, DataValue, DataValue> {
        var usage = 0L
        var rest = 0L

        var total = 0L
        list.filter { it.type != DataBytes.DataType.National }.forEach {
            total += it.initialBytes
            usage += (it.initialBytes - it.bytes)
            rest += it.bytes
        }

        val percent = DateCalendarUtils.calculatePercent(total.toDouble(), rest.toDouble())

        return Triple(percent, DataUnitBytes(rest).getValue(), DataUnitBytes(usage).getValue())
    }


    private suspend fun obtainAverages(userData: List<UserDataBytes>){
        val rest = statisticsManager.getRemainder(TimeUnit.DAYS, userData)
        val usage = statisticsManager.getAverage(System.currentTimeMillis() - DateUtils.MILLIS_PER_DAY * 7, System.currentTimeMillis(), TimeUnit.DAYS)

        liveAverages.postValue(usage to rest)
    }

    fun synchronizeUserDataBytes(callback: SynchronizationResult) {
        viewModelScope.launch {
            try {
                simManager.getDefaultSim(SimDelegate.SimType.VOICE)?.let {
                    synchronizationManager.synchronizeUserDataBytes(it)
                }
                withContext(Dispatchers.Main) {
                    callback.onSuccess()
                }
            } catch (e: Exception) {
                if (e is USSDRequestException) {
                    when (e.errorCode) {
                        USSDHelper.ACCESSIBILITY_SERVICE_UNAVAILABLE -> {
                            withContext(Dispatchers.Main) {
                                callback.onAccessibilityServiceDisabled()
                            }
                        }
                        USSDHelper.DENIED_CALL_PERMISSION -> {
                            withContext(Dispatchers.Main) {
                                callback.onCallPermissionsDenied()
                            }
                        }
                        else -> {
                            withContext(Dispatchers.Main) {
                                callback.onUSSDFail(
                                    (e.message ?: e.errorCode).toString()
                                )
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback.onFailed(e.cause)
                    }
                }
            }
        }
    }


    private fun filter(userData: List<UserDataBytes>): List<UserDataBytes> {
        return when (filter) {
            FilterUserDataBytes.SIZE_ASC -> {
                userData.sortedBy { it.bytes }
            }
            FilterUserDataBytes.SIZE_DESC -> {
                userData.sortedByDescending { it.bytes }
            }
            FilterUserDataBytes.EXPIRE_ASC -> {
                userData.sortedBy { it.expiredTime }
            }
            FilterUserDataBytes.EXPIRE_DESC -> {
                userData.sortedByDescending { it.expiredTime }
            }
            else -> userData
        }.filter { it.exists() && !it.isExpired() }
    }


    interface SynchronizationResult {
        fun onSuccess()
        fun onCallPermissionsDenied()
        fun onUSSDFail(message: String)
        fun onFailed(throwable: Throwable?)
        fun onAccessibilityServiceDisabled()
    }

    enum class FilterUserDataBytes {
        SIZE_DESC,
        SIZE_ASC,
        EXPIRE_ASC,
        EXPIRE_DESC,
        NORMAL
    }

}